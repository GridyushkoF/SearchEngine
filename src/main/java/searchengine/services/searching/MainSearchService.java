package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResult;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.services.lemmas.LemmaService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class MainSearchService {
    private final LemmaService lemmaService;
    private final SearchServiceCacheProxy searchServiceCacheProxy;
    private final SearchQueryFilterService searchQueryFilterService;

    public Optional<List<SearchResult>> searchByQuery(String query, String boundedSiteUrl) {
        Set<String> allQueryLemmas = lemmaService.getExtractor().getUniqueStringLemmasFromText(query);
        List<Lemma> filteredQueryLemmas = searchQueryFilterService.getFilteredAndSortedByFrequencyLemmas(allQueryLemmas);
        if (filteredQueryLemmas.isEmpty()) {
            return Optional.empty();
        }
        List<Page> suitablePages = searchServiceCacheProxy.getSuitablePages(filteredQueryLemmas);
        Map<Page, Float> pages2Relevance = searchServiceCacheProxy.getSortedPages2Relevance(suitablePages);
        return Optional.of(
                sortSearchResultsBySnippetRelevance(searchServiceCacheProxy
                        .getSearchResults(pages2Relevance, allQueryLemmas, boundedSiteUrl), allQueryLemmas)
        );
    }

    private List<SearchResult> sortSearchResultsBySnippetRelevance(List<SearchResult> searchResults, Set<String> notFilteredLemmasInQuery) {
        Map<SearchResult, Integer> searchResult2SnippetRelevance = getSearchResults2snippetRelevance(searchResults, notFilteredLemmasInQuery);
        return searchResults.stream()
                .sorted((searchResult, searchResult2) -> searchResult2SnippetRelevance.get(searchResult2).compareTo(searchResult2SnippetRelevance.get(searchResult)))
                .collect(Collectors.toList());
    }

    private Map<SearchResult, Integer> getSearchResults2snippetRelevance(List<SearchResult> searchResults, Set<String> notFilteredLemmasInQuery) {
        Map<SearchResult, Integer> searchResult2SnippetRelevance = new HashMap<>();
        List<String> tempVisitedLemmas = new ArrayList<>();
        for (SearchResult searchResult : searchResults) {
            Set<String> snippetWords = lemmaService.getExtractor()
                    .getUniqueStringLemmasFromText(searchResult.getSnippet());
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
