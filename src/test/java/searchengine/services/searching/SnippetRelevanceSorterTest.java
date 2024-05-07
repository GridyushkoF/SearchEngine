package searchengine.services.searching;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import searchengine.dto.search.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class SnippetRelevanceSorterTest {
    private SnippetRelevanceSorter snippetRelevanceSorter;

    @BeforeEach
    void setUp() {
        snippetRelevanceSorter = new SnippetRelevanceSorter();
    }
    @Test
    void testSortSearchResultsBySnippetRelevance() {
        List<SearchResult> searchResults = new ArrayList<>();
        SearchResult lowRelevanceResult = new SearchResult(
                "someSite",
                "someSite",
                "https://somesite.com",
                "ITS SOME SITE PAGE!",
                "first lemma other data",
                0.83F);
        SearchResult highRelevanceResult = new SearchResult(
                "someSite",
                "someSite",
                "https://somesite.com",
                "ITS SOME SITE 2 PAGE!",
                "first lemma second lemma third lemma other data",
                0.576F);

        SearchResult middleRelevanceResult = new SearchResult(
                "someSite",
                "someSite",
                "https://somesite.com",
                "ITS SOME SITE 3 PAGE!",
                "first lemma second other data",
                0.2456F);
        searchResults.add(lowRelevanceResult);
        searchResults.add(highRelevanceResult);
        searchResults.add(middleRelevanceResult);
        Set<String> testSearchQuery = Set.of("first","second","third");
        List<SearchResult> actualSorted = snippetRelevanceSorter.sortSearchResultsBySnippetRelevance(searchResults,testSearchQuery);
        List<SearchResult> expectedSorted = List.of(highRelevanceResult,middleRelevanceResult,lowRelevanceResult);
        Assertions.assertEquals(expectedSorted,actualSorted);

    }
}