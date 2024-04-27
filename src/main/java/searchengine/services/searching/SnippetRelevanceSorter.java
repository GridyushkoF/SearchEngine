package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResult;
import searchengine.services.lemmas.LemmaService;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SnippetRelevanceSorter {
    private final LemmaService lemmaService;

    public List<SearchResult> sortSearchResultsBySnippetRelevance(List<SearchResult> searchResults, Set<String> allSearchQueryLemmas) {
        Map<SearchResult, Integer> searchResult2SnippetRelevance = getSearchResults2snippetRelevance(searchResults, allSearchQueryLemmas);
        return searchResults.stream()
                .sorted((searchResult, searchResult2) -> searchResult2SnippetRelevance.get(searchResult2).compareTo(searchResult2SnippetRelevance.get(searchResult)))
                .collect(Collectors.toList());
    }

    private Map<SearchResult, Integer> getSearchResults2snippetRelevance(List<SearchResult> searchResults, Set<String> notFilteredLemmasInQuery) {
        Map<SearchResult, Integer> searchResult2SnippetRelevance = new HashMap<>();
        List<String> tempVisitedLemmas = new ArrayList<>();
        for (SearchResult searchResult : searchResults) {
            Set<String> snippetWords = lemmaService.getExtractor()
                    .getUniqueLemmasFromText(searchResult.getSnippet());
            int currentRelevance = getSnippetRelevance(notFilteredLemmasInQuery, tempVisitedLemmas, snippetWords);
            tempVisitedLemmas.clear();
            searchResult2SnippetRelevance.put(searchResult, currentRelevance);
        }
        return searchResult2SnippetRelevance;
    }

    private int getSnippetRelevance(Set<String> allSearchQueryLemmas, List<String> visitedLemmas, Set<String> snippetWords) {
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
