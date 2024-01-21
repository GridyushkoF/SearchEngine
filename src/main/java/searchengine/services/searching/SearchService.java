package searchengine.services.searching;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResult;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.LemmatizationService;
import searchengine.util.IndexingUtils;
import searchengine.util.LogMarkers;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class SearchService {
    private static final int MAX_LEMMA_FREQUENCY_PERCENT = 50;
    private final LemmatizationService lemmatizationService;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SearchIndexRepository indexRepo;

    @Autowired
    public SearchService(LemmatizationService lemmatizationService, LemmaRepository lemmaRepository, PageRepository pageRepository, SearchIndexRepository indexRepo, SiteRepository siteRepository) {
        this.lemmatizationService = lemmatizationService;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepo = indexRepo;
    }

    @Transactional
    public List<SearchResult> search(String query, String site) {
        //INIT:
        List<Page> pagesOfRarestLemmaModel = new ArrayList<>();
        List<Page> suitablePages = new ArrayList<>();
        Map<Page, Float> dbPages2Relevance = new HashMap<>();
        List<SearchResult> searchResults = new ArrayList<>();
        List<Lemma> filtredLemmaList = filterLemmasByFrequency(lemmatizationService.getLemmasSet(query), site);
        filtredLemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));//ASC SORT
        Lemma rarestLemmaByQuery = filtredLemmaList.get(0);
        //LISTS FILLING:
        indexRepo.findAllByLemma(rarestLemmaByQuery).stream().map(SearchIndex::getPage).forEach(pagesOfRarestLemmaModel::add);
        filtredLemmaList
                .stream()
                .skip(1)
                .forEach(lemmaModel -> {
                    indexRepo.findAllByLemma(lemmaModel)
                            .stream()
                            .map(SearchIndex::getPage)
                            .filter(pagesOfRarestLemmaModel::contains).forEach(suitablePages::add);
                });
        dbPages2Relevance = fillMapAndCalculateRelevance(suitablePages, dbPages2Relevance);
        return getSearchResults(dbPages2Relevance, filtredLemmaList);
    }

    private Map<Page, Float> fillMapAndCalculateRelevance(List<Page> suitablePages, Map<Page, Float> dbPages2Relevance) {
        float maxAbsoluteRelevance = 0;
        float prevAbsoluteRelevance = 0;
        for (Page page : suitablePages) {
            float currentAbsoluteRelevance = getAbsoluteRelevance(page);
            dbPages2Relevance.put(page, currentAbsoluteRelevance);
            maxAbsoluteRelevance = Math.max(prevAbsoluteRelevance, currentAbsoluteRelevance);
            prevAbsoluteRelevance = currentAbsoluteRelevance;
        }
        for (Page pageKey : dbPages2Relevance.keySet()) {
            dbPages2Relevance.replace(pageKey, dbPages2Relevance.get(pageKey) / maxAbsoluteRelevance);
        }
        dbPages2Relevance = sortMapByRelevance(dbPages2Relevance);
        return dbPages2Relevance;
    }

    private List<SearchResult> getSearchResults(Map<Page, Float> dbPages2Relevance, List<Lemma> filtredLemmaList) {
        List<SearchResult> searchResults = new ArrayList<>();
        dbPages2Relevance.forEach((PageModel, relevance) -> {
            SearchResult result = null;
            try {
                result = new SearchResult(
                        PageModel.getSite().getUrl(),
                        IndexingUtils.getSiteName(PageModel.getSite().getUrl()),
                        PageModel.getPath(),
                        IndexingUtils.getTitleOf(PageModel.getContent()),
                        getHtmlSnippet(PageModel, filtredLemmaList),
                        relevance
                );
            } catch (Exception e) {
                log.error(LogMarkers.EXCEPTIONS,"Exception while searching: ", e);
            }
            searchResults.add(result);
        });
        return searchResults;
    }
    public float getAbsoluteRelevance(Page page) {
        return indexRepo
                .findAllByPage(page)
                .stream()
                .map(SearchIndex::getRanking)
                .reduce(0, Integer::sum);
    }
    public List<Lemma> filterLemmasByFrequency(Set<String> lemmaStringSet, String boundToSite) {
        return lemmaStringSet.stream()
                .map(lemmaRepository::findByLemma)
                .flatMap(Optional::stream)
                .filter(lemmaModel -> {
                    Site siteOfLemmaModel = lemmaModel.getSite();
                    long pagesAmount = pageRepository.countPageBySite(siteOfLemmaModel);
                    float percent = (float) (lemmaModel.getFrequency() * 100) / pagesAmount;
                    System.err.println("Лемма: " + lemmaModel.getLemma() + "\n\tПроцент: " + percent);
                    if (boundToSite == null) {
                        return percent <= MAX_LEMMA_FREQUENCY_PERCENT;
                    }
                    return percent <= MAX_LEMMA_FREQUENCY_PERCENT && siteOfLemmaModel.getUrl().equals(boundToSite);
                })
                .collect(Collectors.toList());
    }
    public Map<Page, Float> sortMapByRelevance(Map<Page, Float> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.<Page, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }
    public String getHtmlSnippet(Page Page, List<Lemma> lemma) {
        Set<String> lemmasStringSet = lemma.stream()
                .map(Lemma::getLemma)
                .collect(Collectors.toSet());
        String content = LemmatizationService.removeTagsWithoutNormalization(Page.getContent());
        List<String> contentWords = List.of(content.split(" "));
        List<String> contentWithSelectedLemmas = new ArrayList<>();
        for (String contentWord : contentWords) {
            if (!contentWord.isEmpty()) {
                String normalizedWord = "";
                if (LemmatizationService.isCyrillic(contentWord)
                ) {
                    List<String> wordForms = LemmatizationService.getNormalForms(contentWord);
                    if (wordForms != null) {
                        normalizedWord = wordForms.get(0);
                    }
                } else {
                    normalizedWord = contentWord.toLowerCase();
                }
                contentWithSelectedLemmas.add(lemmasStringSet.contains(normalizedWord) ? ("<b>" + contentWord + "</b>") : contentWord);
            }

        }
        contentWithSelectedLemmas = normalizeHtmlSnippet(contentWithSelectedLemmas);
        String snippet;
        int selectedPhraseId = 0;
        int maxLength = 0;
        for (int i = 0; i < contentWithSelectedLemmas.size(); i++) {
            String word = contentWithSelectedLemmas.get(i);
            if (isSelectedWord(word)) {
                if (word.length() > maxLength) {
                    maxLength = word.length();
                    selectedPhraseId = i;
                }
            }
        }
        int startSublistIndex = Math.max(0, selectedPhraseId - 10);
        int endSubListIndex = Math.min(contentWithSelectedLemmas.size(), selectedPhraseId + 10);
        snippet = contentWithSelectedLemmas.subList(startSublistIndex, endSubListIndex)
                .toString().replaceAll("[\\[\\],]", "");
        return sliceSnippetByLimit(snippet);
    }
    public List<String> normalizeHtmlSnippet(List<String> wordList) {
        List<String> normalizedWords = new ArrayList<>();
        boolean includeNextWord = true;
        for (int i = 0; i < wordList.size(); i++) {
            if (!includeNextWord) {
                includeNextWord = true;
                continue;
            }
            String currentWord = wordList.get(i);
            if (i + 1 >= wordList.size()) {
                normalizedWords.add(currentWord);
                break;
            }
            String nextWord = wordList.get(i + 1);
            boolean isCurrentAndNextWordsSelected = isSelectedWord(currentWord) && isSelectedWord(nextWord);

            if (isCurrentAndNextWordsSelected) {
                currentWord = currentWord.replaceAll("</b>", "");
                nextWord = nextWord.replaceAll("<b>", "");
                currentWord += " " + nextWord;
                includeNextWord = false;
            }
            normalizedWords.add(currentWord);
        }
        return normalizedWords;
    }

    public String sliceSnippetByLimit(String htmlSnippet) {
        int limit = 120;
        if (htmlSnippet.length() >= limit) {
            StringBuilder newSnippet = new StringBuilder();
            boolean isSplicePlace = false;
            for (int i = 0; i < htmlSnippet.length(); i++) {
                char currentSymbol = htmlSnippet.charAt(i);
                if (i % limit == 0 && i > 0) {
                    isSplicePlace = true;
                }
                if (htmlSnippet.charAt(i) == ' ' && isSplicePlace) {
                    isSplicePlace = false;
                    newSnippet.append(currentSymbol).append("\n");
                } else {
                    newSnippet.append(currentSymbol);
                }

            }
            return newSnippet.toString();
        }
        return htmlSnippet;

    }

    public boolean isSelectedWord(String word) {
        return word.startsWith("<b>") && word.endsWith("</b>");
    }

}
