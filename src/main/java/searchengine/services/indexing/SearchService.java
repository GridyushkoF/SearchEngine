package searchengine.services.indexing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.services.RepoService;
import searchengine.services.lemmas.LemmatizationService;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private final LemmatizationService lemmatizationService;
    private final RepoService repoService;
    private static final int MAX_LEMMA_FREQUENCY_PERCENT = 50;
    private final SearchIndexRepository indexRepo;

    @Autowired
    public SearchService(RepoService repoService) {
        this.repoService = repoService;
        this.lemmatizationService = new LemmatizationService(repoService);
        this.indexRepo = repoService.getIndexRepo();
    }
    @Transactional
    public List<SearchResult> search (String query,String site) {

        List<Lemma> lemmaList = getLemmasWhereFrequencyLowerMaxPercent(
                lemmatizationService.getLemmasSet(query),site);

        lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        List<Page> rarestLemmaPages = new ArrayList<>();
        List<Page> suitablePages = new ArrayList<>();
        Map<Page,Float> pages2Relevance = new HashMap<>();
        List<SearchResult> searchResults = new ArrayList<>();
        indexRepo.findAllByLemma(lemmaList.get(0))
                .forEach(index -> rarestLemmaPages.add(index.getPage()));
        lemmaList.stream().skip(1).forEach(lemma -> {
            indexRepo.findAllByLemma(lemma)
                    .stream()
                    .map(SearchIndex::getPage)
                    .filter(rarestLemmaPages::contains).forEach(suitablePages::add);});
        float lastRanking = 0;
        float maxAbsoluteRelevance = 0;
        for (var page : suitablePages) {
            var currentAbsRel = getAbsoluteRelevanceOf(page);
            pages2Relevance.put(page,currentAbsRel);
            maxAbsoluteRelevance = Math.max(lastRanking, currentAbsRel);
            lastRanking = currentAbsRel;
        }
        for (Map.Entry<Page,Float> entry : pages2Relevance.entrySet()) {
            pages2Relevance.replace(entry.getKey(),entry.getValue()/maxAbsoluteRelevance);
        }
        pages2Relevance = getSortedMapByValue(pages2Relevance);
        pages2Relevance.forEach((page,relevance) -> {
            SearchResult result = null;
            try {
                result = new SearchResult(
                        page.getPath(),
                        IndexUtils.getTitleOf(page.getContent()),
                        getHtmlSnippet(page,lemmaList),
                        relevance
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            searchResults.add(result);
        });
        return searchResults;
    }

    public float getAbsoluteRelevanceOf(Page page) {
        return repoService.getIndexRepo()
                .findAllByPage(page)
                .stream()
                .map(SearchIndex::getRanking)
                .reduce(0, Integer::sum);
    }
    public List<Lemma> getLemmasWhereFrequencyLowerMaxPercent(Set<String> lemmaStringSet,String boundToSite) {
        return lemmaStringSet.stream()
                .map(repoService.getLemmaRepo()::findByLemma)
                .flatMap(Optional::stream)
                .filter(DBLemma -> {
                    if(boundToSite == null) {
                        return filterLemmas(DBLemma);
                    } else {
                        if(!DBLemma.getSite().getUrl().equals(boundToSite)) {
                            return false;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
    private boolean filterLemmas (Lemma DBLemma) {
        Site DBLemmaSite = DBLemma.getSite();
        long pagesAmount = repoService
                .getPageRepo().countPageBySite(DBLemmaSite);
        float percent =  (float) (DBLemma.getFrequency() * 100) / pagesAmount;
        System.err.println("Лемма: " + DBLemma.getLemma() + "\n\tПроцент: " + percent);
        return percent <= MAX_LEMMA_FREQUENCY_PERCENT;
    }
    public Map<Page,Float> getSortedMapByValue (Map<Page,Float> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.<Page,Float>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }
    public String getHtmlSnippet(Page page, List<Lemma> lemmas) {
        Set<String> lemmaStringList = lemmas.stream()
                .map(Lemma::getLemma)
                .collect(Collectors.toSet());
        String content = LemmatizationService.removeTagsWithoutNormalization(page.getContent());
        System.out.println(content);
        List<String> contentWords = List.of(content.split(" "));
        List<String> contentWithSelectedLemmas = new ArrayList<>();
        var snippet = new StringJoiner(",");
        for (String contentWord : contentWords) {
            if(!contentWord.isEmpty()) {
                if (LemmatizationService.isCyrillic(contentWord)
                ) {
                    List<String> wordForms =  LemmatizationService.getNormalForms(contentWord);
                    if (wordForms != null) {
                        String normalizedWord = wordForms.get(0);
                        contentWithSelectedLemmas.add(lemmaStringList.contains(normalizedWord) ? ("<b>" + contentWord + "</b>") : contentWord);
                    }
                } else {contentWithSelectedLemmas.add(contentWord);}
            }

        }
        contentWithSelectedLemmas = normalizeSnippet(contentWithSelectedLemmas);
        for (int i = 0; i < contentWithSelectedLemmas.size(); i++) {
            if (snippet.length() <= 200) {
                String word = contentWithSelectedLemmas.get(i);
                if (word.startsWith("<b>")) {
                    int startIndex = Math.max(0,i - 5);
                    int endIndex = Math.min(contentWithSelectedLemmas.size() - 1,i + 5);
                    snippet.add(contentWithSelectedLemmas
                            .subList(startIndex,endIndex)
                            .toString().replaceAll("[\\[\\],]",""));
                }
            } else {
                snippet.add("...");
                break;
            }
        }
        return sliceSnippetByLimit(snippet.toString());
    }
    public List<String> normalizeSnippet(List<String> allWords) {
        List<String> normalizedWords = new ArrayList<>();
        ListIterator<String> iterator = allWords.listIterator();
        while (iterator.hasNext()) {
            var currentWord = iterator.next();
            if (iterator.hasNext()) {
                var nextWord = iterator.next();
                if (isWordSelected(currentWord) && isWordSelected(nextWord)) {
                    currentWord = currentWord.replace("</b>","");
                    nextWord = nextWord.replace("<b>","");
                    currentWord += " " + nextWord;
                    // Пропустить следующую итерацию
                    iterator.next();
                }
            }
            normalizedWords.add(currentWord);
        }
        return normalizedWords;
    }
    public String sliceSnippetByLimit(String snippet) {
        int limit = 120;
        if (snippet.length() >= limit) {
            StringBuilder newSnippet = new StringBuilder();
            boolean isSplicePlace = false;
            for (int i = 0; i < snippet.length(); i++) {
                char currentSymbol = snippet.charAt(i);
                if (i % limit == 0 && i > 0) {isSplicePlace = true;}
                if (snippet.charAt(i) == ' ' && isSplicePlace) {
                    isSplicePlace = false;
                    newSnippet.append(currentSymbol).append("\n");
                } else {newSnippet.append(currentSymbol);}

            }
            return newSnippet.toString();
        }
        return snippet;

    }
    public boolean isWordSelected(String word) {
        return word.startsWith("<b>") && word.endsWith("</b>");
    }

}
