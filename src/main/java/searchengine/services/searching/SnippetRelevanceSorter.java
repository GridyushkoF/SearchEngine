package searchengine.services.searching;

import searchengine.dto.search.SearchResult;
import searchengine.util.LemmasExtractorUtil;

import java.util.*;
import java.util.stream.Collectors;



public class SnippetRelevanceSorter {
    private final LemmasExtractorUtil extractor = new LemmasExtractorUtil();

    public List<SearchResult> sortSearchResultsBySnippetRelevance(List<SearchResult> searchResults, Set<String> allSearchQueryLemmas) {
        Map<SearchResult, Integer> searchResult2SnippetRelevance = getSearchResults2snippetRelevance(searchResults, allSearchQueryLemmas);
        return searchResults.stream()
                .sorted((searchResult, searchResult2) -> searchResult2SnippetRelevance
                .get(searchResult2).compareTo(searchResult2SnippetRelevance.get(searchResult)))
                .collect(Collectors.toList());
    }

    private Map<SearchResult, Integer> getSearchResults2snippetRelevance(List<SearchResult> searchResults, Set<String> notFilteredLemmasInQuery) {
        Map<SearchResult, Integer> searchResult2SnippetRelevance = new HashMap<>();
        for (SearchResult searchResult : searchResults) {
            Set<String> snippetWords = extractor.getUniqueLemmasFromText(searchResult.getSnippet());
            int currentRelevance = getSnippetRelevance(notFilteredLemmasInQuery, snippetWords);
            searchResult2SnippetRelevance.put(searchResult, currentRelevance);
        }
        return searchResult2SnippetRelevance;
    }

    private int getSnippetRelevance(Set<String> allSearchQueryLemmas, Set<String> snippetWords) {
        List<String> visitedLemmas = new ArrayList<>();
        int currentRelevance = 0;
        for (String lemma : allSearchQueryLemmas) {
            if (snippetWords.contains(lemma) && !visitedLemmas.contains(lemma)) {
                visitedLemmas.add(lemma);
                currentRelevance++;
            }
        }
        return currentRelevance;
    }
}
