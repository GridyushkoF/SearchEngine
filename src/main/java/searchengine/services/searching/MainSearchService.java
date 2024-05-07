package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResult;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.LogMarkersUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Log4j2
@RequiredArgsConstructor
public class MainSearchService {
    private final LemmaService lemmaService;
    private final SearchServiceCacheProxy searchServiceCacheProxy;
    private final SearchQueryFilterService searchQueryFilterService;
    private final SnippetRelevanceSorter snippetRelevanceSorter = new SnippetRelevanceSorter();
    public Optional<List<SearchResult>> searchByQuery(String query, String boundedSiteUrl) {
        log.info(LogMarkersUtil.INFO,"Запущен поисковый запрос: " + query + ", " + ((boundedSiteUrl == null) ? ("не привязанный к сайту") : ("привязанный к сайту " + boundedSiteUrl)));
        Set<String> allQueryLemmas = lemmaService.getExtractor().getUniqueLemmasFromText(query);
        List<LemmaEntity> filteredQueryLemmaEntities = searchQueryFilterService.getFilteredAndSortedByFrequencyLemmas(allQueryLemmas);
        log.info(LogMarkersUtil.INFO,"filtered lemmas: " + filteredQueryLemmaEntities);
        if (filteredQueryLemmaEntities.isEmpty()) {
            return Optional.empty();
        }
        List<PageEntity> suitablePages = searchServiceCacheProxy.getSuitablePages(filteredQueryLemmaEntities);
        log.info(LogMarkersUtil.INFO,"SUITABLE PAGES FOUND: " + suitablePages);
        List<SearchResult> searchResults = searchServiceCacheProxy.getSearchResults(suitablePages,allQueryLemmas, boundedSiteUrl);
        return Optional.of(
                snippetRelevanceSorter.sortSearchResultsBySnippetRelevance(searchResults, allQueryLemmas)
        );
    }
}
