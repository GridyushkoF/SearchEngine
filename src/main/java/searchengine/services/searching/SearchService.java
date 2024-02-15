package searchengine.services.searching;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
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
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.LemmatizationService;
import searchengine.util.IndexingUtils;
import searchengine.util.LogMarkers;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class SearchService {
    private static final int MAX_LEMMA_FREQUENCY_PERCENT = 50;
    private final LemmatizationService lemmatizationService;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SearchIndexRepository indexRepository;
    private final SnippetExtractor snippetExtractor;

    @Autowired
    public SearchService(LemmatizationService lemmatizationService, LemmaRepository lemmaRepository, PageRepository pageRepository, SearchIndexRepository indexRepository, SiteRepository siteRepository) {
        this.lemmatizationService = lemmatizationService;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.snippetExtractor = new SnippetExtractor();
    }

    @Transactional
    public List<SearchResult> search(String query, String site) {
        //INIT:
        List<Page> pagesOfRarestLemmaModel = new ArrayList<>();
        List<Page> suitablePages = new ArrayList<>();
        Map<Page, Float> modelPages2Relevance = new HashMap<>();
        Set<String> notFilteredStringLemmaList = lemmatizationService.getLemmasSet(query);
        List<Lemma> filtredLemmaList = filterLemmasByFrequency(lemmatizationService.getLemmasSet(query), site);
        filtredLemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));//ASC SORT
        Lemma rarestLemmaByQuery = filtredLemmaList.get(0);
        //LISTS FILLING:
        indexRepository.findAllByLemma(rarestLemmaByQuery).stream().map(SearchIndex::getPage).forEach(pagesOfRarestLemmaModel::add);
        filtredLemmaList
                .stream()
                .skip(1)
                .forEach(lemmaModel -> {
                    indexRepository.findAllByLemma(lemmaModel)
                            .stream()
                            .map(SearchIndex::getPage)
                            .filter(pagesOfRarestLemmaModel::contains).forEach(suitablePages::add);
                });
        modelPages2Relevance = calculateRelevanceByPages(suitablePages, modelPages2Relevance);
        return getSearchResults(modelPages2Relevance, notFilteredStringLemmaList);
    }

    private Map<Page, Float> calculateRelevanceByPages(List<Page> suitablePages, Map<Page, Float> dbPages2Relevance) {
        float maxAbsoluteRelevance = 0;
        float prevAbsoluteRelevance = 0;
        for (Page page : suitablePages) {
            float currentAbsoluteRelevance = getAbsoluteRelevance(page);
            dbPages2Relevance.put(page, currentAbsoluteRelevance);
            maxAbsoluteRelevance = Math.max(prevAbsoluteRelevance, currentAbsoluteRelevance);
            prevAbsoluteRelevance = currentAbsoluteRelevance;
        }
        for (Page pageKey : dbPages2Relevance.keySet()) {
            dbPages2Relevance.replace(pageKey, dbPages2Relevance.get(pageKey) / maxAbsoluteRelevance);
        }
        dbPages2Relevance = sortPagesMapByRelevance(dbPages2Relevance);
        return dbPages2Relevance;
    }

    private List<SearchResult> getSearchResults(Map<Page, Float> dbPages2Relevance, Set<String> lemmaStringList) {
        List<SearchResult> searchResults = new ArrayList<>();
        dbPages2Relevance.forEach((pageModel, relevance) -> {
            SearchResult result = null;
            try {
                result = new SearchResult(
                        pageModel.getSite().getUrl(),
                        IndexingUtils.getSiteName(pageModel.getSite().getUrl()),
                        pageModel.getPath(),
                        IndexingUtils.getTitleOf(pageModel.getContent()),
                        snippetExtractor.extractHtmlSnippet(pageModel, lemmaStringList),
                        relevance
                );
            } catch (Exception e) {
                log.error(LogMarkers.EXCEPTIONS,"Exception while searching: ", e);
            }
            searchResults.add(result);
        });
        return searchResults;
    }
    public float getAbsoluteRelevance(Page page) {
        return indexRepository
                .findAllByPage(page)
                .stream()
                .map(SearchIndex::getRanking)
                .reduce(0, Integer::sum);
    }
    public List<Lemma> filterLemmasByFrequency(Set<String> lemmaStringSet, String boundToSite) {
        return lemmaStringSet.stream()
                .map(lemmaRepository::findByLemma)
                .flatMap(Optional::stream)
                .filter(lemmaModel -> {
                    Site siteOfLemmaModel = lemmaModel.getSite();
                    long pagesAmount = pageRepository.countIndexedPagesBySite(siteOfLemmaModel);
                    float percent = (float) (lemmaModel.getFrequency() * 100) / pagesAmount;
                    System.err.println("Лемма: " + lemmaModel.getLemma() + "\n\tПроцент: " + percent);
                    if (boundToSite == null) {
                        return percent <= MAX_LEMMA_FREQUENCY_PERCENT;
                    }
                    return percent <= MAX_LEMMA_FREQUENCY_PERCENT && siteOfLemmaModel.getUrl().equals(boundToSite);
                })
                .collect(Collectors.toList());
    }
    public Map<Page, Float> sortPagesMapByRelevance(Map<Page, Float> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.<Page, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }
}
