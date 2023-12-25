package searchengine.services.searching;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.model.*;
import searchengine.repositories.SearchIndexRepo;
import searchengine.services.other.RepoService;
import searchengine.services.other.IndexingUtils;
import searchengine.services.lemmas.LemmatizationService;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private final LemmatizationService lemmatizationService;
    private final RepoService repoService;
    private static final int MAX_LEMMA_FREQUENCY_PERCENT = 50;
    private final SearchIndexRepo indexRepo;

    @Autowired
    public SearchService(RepoService repoService) {
        this.repoService = repoService;
        this.lemmatizationService = new LemmatizationService(repoService);
        this.indexRepo = repoService.getIndexRepo();
    }
    @Transactional
    public List<SearchData> search (String query, String site) {
        //variables initialization :
        List<Lemma> lemmaList = filterLemmasByFrequency(lemmatizationService.getLemmasSet(query),site);
        lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));

        List<Page> pagesOfRarestDBLemma = new ArrayList<>();
        List<Page> suitablePages = new ArrayList<>();
        Map<Page,Float> DBPages2Relevance = new HashMap<>();
        List<SearchData> searchResults = new ArrayList<>();
        float lastAbsRel = 0;
        float maxAbsoluteRelevance = 0;
        Lemma rarestLemmaByQuery = lemmaList.get(0);
        //lists filling:
        indexRepo.findAllByLemma(rarestLemmaByQuery)
                .forEach(searchIndex -> pagesOfRarestDBLemma.add(searchIndex.getPage()));
        lemmaList.stream().skip(1).forEach(DBLemma -> {
            indexRepo.findAllByLemma(DBLemma)
                    .stream()
                    .map(SearchIndex::getPage)
                    .filter(pagesOfRarestDBLemma::contains).forEach(suitablePages::add);});
        for (var page : suitablePages) {
            float currentAbsRel = getAbsoluteRelevance(page);
            DBPages2Relevance.put(page,currentAbsRel);
            maxAbsoluteRelevance = Math.max(lastAbsRel, currentAbsRel);
            lastAbsRel = currentAbsRel;
        }
        for (var key : DBPages2Relevance.keySet()) {
            DBPages2Relevance.replace(key,DBPages2Relevance.get(key) / maxAbsoluteRelevance);
        }
        DBPages2Relevance = sortMapByRelevance(DBPages2Relevance);
        DBPages2Relevance.forEach((DBPage,relevance) -> {
            SearchData result = null;
            try {
                result = new SearchData(
                        DBPage.getSite().getUrl(),
                        IndexingUtils.getSiteName(DBPage.getSite().getUrl()),
                        DBPage.getPath(),
                        IndexingUtils.getTitleOf(DBPage.getContent()),
                        getHtmlSnippet(DBPage, lemmaList),
                        relevance

                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            searchResults.add(result);
        });
        return searchResults;
    }

    public float getAbsoluteRelevance(Page page) {
        return repoService.getIndexRepo()
                .findAllByPage(page)
                .stream()
                .map(SearchIndex::getRanking)
                .reduce(0, Integer::sum);
    }
    public List<Lemma> filterLemmasByFrequency(Set<String> lemmaStringSet, String boundToSite) {
        return lemmaStringSet.stream()
                .map(repoService.getLemmaRepo()::findByLemma)
                .flatMap(Optional::stream)
                .filter(DBLemma -> {
                    Site DBLemmaSite = DBLemma.getSite();
                    long pagesAmount = repoService
                            .getPageRepo().countPageBySite(DBLemmaSite);
                    float percent =  (float) (DBLemma.getFrequency() * 100) / pagesAmount;
                    System.err.println("Лемма: " + DBLemma.getLemma() + "\n\tПроцент: " + percent);
                    if (boundToSite == null) {
                        return percent <= MAX_LEMMA_FREQUENCY_PERCENT;
                    }
                    return percent <= MAX_LEMMA_FREQUENCY_PERCENT && DBLemmaSite.getUrl().equals(boundToSite);
                })
                .collect(Collectors.toList());
    }
    public Map<Page,Float> sortMapByRelevance(Map<Page,Float> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.<Page,Float>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }
    public String getHtmlSnippet(Page Page, List<Lemma> lemma) {
        Set<String> lemmasStringSet = lemma.stream()
                .map(Lemma::getLemma)
                .collect(Collectors.toSet());
        String content = LemmatizationService.removeTagsWithoutNormalization(Page.getContent());
        System.out.println(content);
        List<String> contentWords = List.of(content.split(" "));
        List<String> contentWithSelectedLemmas = new ArrayList<>();
        for (String contentWord : contentWords) {
            if(!contentWord.isEmpty()) {
                String normalizedWord = "";
                if (LemmatizationService.isCyrillic(contentWord)
                ) {
                    List<String> wordForms =  LemmatizationService.getNormalForms(contentWord);
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
            if(isSelectedWord(word)) {
                if (word.length() > maxLength) {
                    maxLength = word.length();
                    selectedPhraseId = i;
                }
            }
        }
        int startSublistIndex = Math.max(0,selectedPhraseId - 10);
        int endSubListIndex = Math.min(contentWithSelectedLemmas.size(),selectedPhraseId + 10);
        snippet = contentWithSelectedLemmas.subList(startSublistIndex, endSubListIndex)
                .toString().replaceAll("[\\[\\],]","");
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
                currentWord = currentWord.replaceAll("</b>","");
                nextWord = nextWord.replaceAll("<b>","");
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
                if (i % limit == 0 && i > 0) {isSplicePlace = true;}
                if (htmlSnippet.charAt(i) == ' ' && isSplicePlace) {
                    isSplicePlace = false;
                    newSnippet.append(currentSymbol).append("\n");
                } else {newSnippet.append(currentSymbol);}

            }
            return newSnippet.toString();
        }
        return htmlSnippet;

    }
    public boolean isSelectedWord(String word) {
        return word.startsWith("<b>") && word.endsWith("</b>");
    }

}
