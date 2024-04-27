package searchengine.services.searching;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResult;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;
import searchengine.util.IndexingUtil;
import searchengine.util.LogMarkersUtil;
import searchengine.util.SnippetExtractorUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Log4j2
public class SearchServiceCacheProxy {
    private final IndexRepository indexRepository;
    private final SnippetExtractorUtil snippetExtractorUtil;
    private final PagesFilterService pagesFilter;

    @Cacheable("suitablePages")
    public List<PageEntity> getSuitablePages(List<LemmaEntity> filteredLemmasFromSearchQuery) {
        LemmaEntity rarestLemmaOfQuery = filteredLemmasFromSearchQuery.get(0);
        List<PageEntity> pagesContainingRarestLemmaOfQuery = indexRepository.findAllByLemmaEntity(rarestLemmaOfQuery).stream()
            .map(IndexEntity::getPage).toList();

        Set<String> filteredLemmas = filteredLemmasFromSearchQuery.stream()
            .map(LemmaEntity::getLemma)
            .collect(Collectors.toSet());

        return pagesFilter.filterPagesContainingAllLemmas(pagesContainingRarestLemmaOfQuery, filteredLemmas);
    }
    @Cacheable("sortedPages2relevance")
    public Map<PageEntity, Float> getSortedPages2Relevance(List<PageEntity> pages) {
        if (pages.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<PageEntity, Float> pages2Relevance = new HashMap<>();
        float maxAbsoluteRelevance = pagesFilter.getMaxAbsoluteRelevance(pages, pages2Relevance);
        pages2Relevance.entrySet().forEach(entry -> entry.setValue(entry.getValue() / maxAbsoluteRelevance));
        return pagesFilter.sortPagesMapByRelevance(pages2Relevance);
    }

    @Cacheable("searchResults")
    public List<SearchResult> getSearchResults(Map<PageEntity, Float> pages2Relevance, Set<String> allSearchQueryLemmas, String boundedSiteUrl) {
        List<SearchResult> searchResults = new ArrayList<>();
        pages2Relevance.forEach((page, relevance) -> {
            try {
                if (page.getSite().getUrl().equals(boundedSiteUrl) || boundedSiteUrl == null) {
                    searchResults.add(new SearchResult(
                        page.getSite().getUrl(),
                        page.getSite().getName(),
                        page.getPath(),
                        IndexingUtil.getTitleOf(page.getContent()),
                        snippetExtractorUtil.getHtmlSnippet(
                            page, allSearchQueryLemmas),
                        relevance));
                }
            } catch (Exception e) {
                log.error(LogMarkersUtil.EXCEPTIONS, "Exception while searching: ", e);
            }
        });
        return searchResults;
    }
}
