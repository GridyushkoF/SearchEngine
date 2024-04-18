package searchengine.services.searching;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.repositories.SearchIndexRepository;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.LemmasExtractorUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PagesFilterService {
    @Autowired
    public PagesFilterService(LemmaService lemmaService, SearchIndexRepository indexRepository) {
        this.indexRepository = indexRepository;
        lemmasExtractorUtil = lemmaService.getExtractor();
    }
    private final SearchIndexRepository indexRepository;
    private final LemmasExtractorUtil lemmasExtractorUtil;

    public List<Page> filterPagesContainingAllLemmas(List<Page> pagesThatContainsRarestLemmaOfQuery, Set<String> filteredLemmasSortedByFrequency) {
        return pagesThatContainsRarestLemmaOfQuery.stream().filter(page -> {
                    Set<String> stringLemmasInPage =
                            lemmasExtractorUtil.getUniqueStringLemmasFromText(lemmasExtractorUtil.removeHtmlTags(page.getContent()));
                    return stringLemmasInPage.containsAll(filteredLemmasSortedByFrequency);
                })
                .toList();
    }
    public Map<Page, Float> sortPagesMapByRelevance(Map<Page, Float> pages2Relevance) {
        return pages2Relevance.entrySet()
                .stream()
                .sorted(Map.Entry.<Page, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public float getAbsoluteRelevance(Page page) {
        return indexRepository
                .findAllByPage(page)
                .stream()
                .map(SearchIndex::getRanking)
                .reduce(0, Integer::sum);
    }

    public float getMaxAbsoluteRelevance(List<Page> pages, Map<Page, Float> pages2Relevance) {
        float maxAbsoluteRelevance = 0;
        for (Page page : pages) {
            float currentAbsoluteRelevance = getAbsoluteRelevance(page);
            pages2Relevance.put(page, currentAbsoluteRelevance);
            maxAbsoluteRelevance = Math.max(maxAbsoluteRelevance, currentAbsoluteRelevance);
        }
        return maxAbsoluteRelevance;
    }
}
