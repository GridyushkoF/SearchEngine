package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.search.SearchResult;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.services.lemmas.LemmaService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Log4j2
@RequiredArgsConstructor
public class MainSearchService {
    private final LemmaService lemmaService;
    private final SearchServiceCacheProxy searchServiceCacheProxy;
    private final SearchQueryFilterService searchQueryFilterService;
    private final SnippetRelevanceSorter snippetRelevanceSorter;
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Optional<List<SearchResult>> searchByQuery(String query, String boundedSiteUrl) {
        Set<String> allQueryLemmas = lemmaService.getExtractor().getUniqueStringLemmasFromText(query);
        List<Lemma> filteredQueryLemmas = searchQueryFilterService.getFilteredAndSortedByFrequencyLemmas(allQueryLemmas);
        if (filteredQueryLemmas.isEmpty()) {
            return Optional.empty();
        }
        List<Page> suitablePages = searchServiceCacheProxy.getSuitablePages(filteredQueryLemmas);
        Map<Page, Float> pages2Relevance = searchServiceCacheProxy.getSortedPages2Relevance(suitablePages);
        return Optional.of(
                snippetRelevanceSorter.sortSearchResultsBySnippetRelevance(searchServiceCacheProxy
                        .getSearchResults(pages2Relevance, allQueryLemmas, boundedSiteUrl), allQueryLemmas)
        );
    }
}
