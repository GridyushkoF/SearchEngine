package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.search.SearchResult;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.services.lemmas.DuplicateFixService;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.LogMarkersUtil;

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
    private final DuplicateFixService duplicateFixService;
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Optional<List<SearchResult>> searchByQuery(String query, String boundedSiteUrl) {
        duplicateFixService.mergeAllDuplicates();
        log.info(LogMarkersUtil.INFO,"Запущен поисковый запрос: " + query + ", " + ((boundedSiteUrl == null) ? ("не привязанный к сайту") : ("привязанный к сайту " + boundedSiteUrl)));
        Set<String> allQueryLemmas = lemmaService.getExtractor().getUniqueLemmasFromText(query);
        List<LemmaEntity> filteredQueryLemmaEntities = searchQueryFilterService.getFilteredAndSortedByFrequencyLemmas(allQueryLemmas);
        if (filteredQueryLemmaEntities.isEmpty()) {
            return Optional.empty();
        }
        List<PageEntity> suitablePages = searchServiceCacheProxy.getSuitablePages(filteredQueryLemmaEntities);
        Map<PageEntity, Float> pages2Relevance = searchServiceCacheProxy.getSortedPages2Relevance(suitablePages);
        return Optional.of(
                snippetRelevanceSorter.sortSearchResultsBySnippetRelevance(searchServiceCacheProxy
                        .getSearchResults(pages2Relevance, allQueryLemmas, boundedSiteUrl), allQueryLemmas)
        );
    }
}
