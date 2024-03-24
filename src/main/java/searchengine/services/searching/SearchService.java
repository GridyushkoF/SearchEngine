package searchengine.services.searching;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResult;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.util.LemmaExtractor;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.IndexingUtils;
import searchengine.util.LogMarkers;
import searchengine.util.SnippetExtractor;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class SearchService {
    private final LemmaService lemmaService;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SearchIndexRepository indexRepository;
    private static final int MAX_LEMMA_FREQUENCY_PERCENT = 70;
    private final SnippetExtractor snippetExtractor;
    @Autowired
    public SearchService(LemmaService lemmaService, LemmaRepository lemmaRepository, PageRepository pageRepository, SearchIndexRepository indexRepository) {
        this.lemmaService = lemmaService;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        snippetExtractor = new SnippetExtractor(this.lemmaService);
    }

    @Transactional
    public List<SearchResult> search(String query, String boundedSiteUrl) {
        Set<String> notFilteredStringLemmaList = getLemmaListByQuery(query);
        List<Lemma> filteredLemmaList = getFilteredLemmaListByQuery(boundedSiteUrl,notFilteredStringLemmaList);
        if(filteredLemmaList.isEmpty()) {
            return null;
        }

        List<Page> rarestLemmaPages = new ArrayList<>();
        Map<Page, Float> pages2Relevance = new HashMap<>();
        LemmaExtractor extractor = lemmaService.getExtractor();
        Set<String> filtredLemmasStringSet = filteredLemmaList
                .stream().map(Lemma::getLemma).collect(Collectors.toSet());
        Lemma rarestLemmaByQuery = filteredLemmaList.get(0);
        indexRepository.findAllByLemma(rarestLemmaByQuery)
                .stream()
                .map(SearchIndex::getPage)
                .forEach(rarestLemmaPages::add);

        List<Page> suitablePages = getSuitablePages(filteredLemmaList, rarestLemmaPages, extractor, filtredLemmasStringSet);
        pages2Relevance = getRelevanceByPages(suitablePages, pages2Relevance);

        return getSearchResults(pages2Relevance, notFilteredStringLemmaList);
    }

    private List<Page> getSuitablePages(List<Lemma> filteredLemmaList, List<Page> rarestLemmaPages, LemmaExtractor extractor, Set<String> filtredLemmasStringSet) {
        List<Page> suitablePages = new ArrayList<>();
        filteredLemmaList
                .stream()
                .skip(filteredLemmaList.size() > 1 ? 1 : 0)
                .forEach(lemmaModel -> indexRepository
                        .findAllByLemma(lemmaModel)
                        .stream()
                        .map(SearchIndex::getPage)
                        .filter(rarestLemmaPages::contains)
                        .filter(page -> {
                            Set<String> pageLemmaStringSet = extractor
                                .getLemmasSetFromText(
                                    extractor.removeHtmlTags(page.getContent()));
                            return pageLemmaStringSet.containsAll(filtredLemmasStringSet);
                        })
                        .forEach(suitablePages::add));
        return suitablePages;
    }

    public List<Lemma> getFilteredLemmaListByQuery(String boundedSiteUrl,Set<String> lemmaStringSet) {
        List<Lemma> filtredLemmaList = filterLemmasByFrequency(lemmaStringSet, boundedSiteUrl);
        filtredLemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        return filtredLemmaList;
    }
    public Set<String> getLemmaListByQuery(String query) {
        return lemmaService
                .getExtractor()
                .getLemmasSetFromText(query);
    }
    public float getAbsoluteRelevance(Page page) {
        return indexRepository
                .findAllByPage(page)
                .stream()
                .map(SearchIndex::getRanking)
                .reduce(0, Integer::sum);
    }

    private Map<Page, Float> getRelevanceByPages(
            List<Page> pages,
           Map<Page, Float> pages2Relevance) {
        float maxAbsoluteRelevance = 0;
        float prevAbsoluteRelevance = 0;
        for (Page page : pages) {
            float currentAbsoluteRelevance = getAbsoluteRelevance(page);
            pages2Relevance.put(page, currentAbsoluteRelevance);
            maxAbsoluteRelevance = Math.max(prevAbsoluteRelevance, currentAbsoluteRelevance);
            prevAbsoluteRelevance = currentAbsoluteRelevance;
        }
        for (Page pageKey : pages2Relevance.keySet()) {
            pages2Relevance.replace(pageKey, pages2Relevance.get(pageKey) / maxAbsoluteRelevance);
        }
        pages2Relevance = sortPagesMapByRelevance(pages2Relevance);
        return pages2Relevance;
    }

    private List<SearchResult> getSearchResults(Map<Page, Float> pages2Relevance, Set<String> lemmaStringList) {
        List<SearchResult> searchResults = new ArrayList<>();
        pages2Relevance.forEach((page, relevance) -> {
            SearchResult result = null;
            try {
                result = new SearchResult(
                        page.getSite().getUrl(),
                        page.getSite().getName(),
                        page.getPath(),
                        IndexingUtils.getTitleOf(page.getContent()),
                        snippetExtractor.getHtmlSnippet(
                                page, lemmaStringList),
                        relevance
                );
            } catch (Exception e) {
                log.error(LogMarkers.EXCEPTIONS,"Exception while searching: ", e);
            }
            searchResults.add(result);
        });
        return searchResults;
    }
    public List<Lemma> filterLemmasByFrequency(Set<String> lemmaStringSet, String boundedSiteUrl) {
        Map<Boolean, List<Lemma>> partitionedLemmas = getPartitionedLemmas(lemmaStringSet, boundedSiteUrl);

        List<Lemma> exceedingLemmas = partitionedLemmas.get(true);
        List<Lemma> nonExceedingLemmas = partitionedLemmas.get(false);

        if (exceedingLemmas.size() >= nonExceedingLemmas.size()) {
            return lemmaStringSet.stream()
                    .map(lemmaRepository::findByLemma)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        }
        return nonExceedingLemmas;
    }

    private Map<Boolean, List<Lemma>> getPartitionedLemmas(Set<String> lemmaStringSet, String boundToSite) {
        return lemmaStringSet.stream()
                .map(lemmaRepository::findByLemma)
                .flatMap(Optional::stream)
                .collect(Collectors.partitioningBy(lemmaModel -> {
                    Site siteOfLemmaModel = lemmaModel.getSite();
                    long pagesAmount = pageRepository.countIndexedPagesBySite(siteOfLemmaModel);
                    float percent = (float) (lemmaModel.getFrequency() * 100) / pagesAmount;
                    System.err.println("Лемма: " + lemmaModel.getLemma() + "\n\tПроцент: " + percent);
                    if(StringUtil.isNumeric(lemmaModel.getLemma())) {
                        return false;
                    }
                    if (boundToSite == null) {
                        return percent > MAX_LEMMA_FREQUENCY_PERCENT;
                    }
                    return percent > MAX_LEMMA_FREQUENCY_PERCENT && siteOfLemmaModel.getUrl().equals(boundToSite);
                }));
    }

    public Map<Page, Float> sortPagesMapByRelevance(Map<Page, Float> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.<Page, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }
}
