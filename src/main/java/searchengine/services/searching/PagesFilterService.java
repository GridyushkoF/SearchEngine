package searchengine.services.searching;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;
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
    public PagesFilterService(LemmaService lemmaService, IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
        lemmasExtractorUtil = lemmaService.getExtractor();
    }
    private final IndexRepository indexRepository;
    private final LemmasExtractorUtil lemmasExtractorUtil;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<PageEntity> filterPagesContainingAllLemmas(List<PageEntity> pagesThatContainsRarestLemmaOfQuery, Set<String> filteredLemmasSortedByFrequency) {
        return pagesThatContainsRarestLemmaOfQuery.stream().filter(page -> {
                    Set<String> lemmasInPage =
                            lemmasExtractorUtil.getUniqueLemmasFromText(lemmasExtractorUtil.removeHtmlTags(page.getContent()));
                    return lemmasInPage.containsAll(filteredLemmasSortedByFrequency);
                })
                .toList();
    }
    public Map<PageEntity, Float> sortPagesMapByRelevance(Map<PageEntity, Float> pages2Relevance) {
        return pages2Relevance.entrySet()
                .stream()
                .sorted(Map.Entry.<PageEntity, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    private float getAbsoluteRelevance(PageEntity page) {
        return indexRepository
                .findAllByPage(page)
                .stream()
                .map(IndexEntity::getRanking)
                .reduce(0, Integer::sum);
    }

    public float getMaxAbsoluteRelevance(List<PageEntity> pages, Map<PageEntity, Float> pages2Relevance) {
        float maxAbsoluteRelevance = 0;
        for (PageEntity page : pages) {
            float currentAbsoluteRelevance = getAbsoluteRelevance(page);
            pages2Relevance.put(page, currentAbsoluteRelevance);
            maxAbsoluteRelevance = Math.max(maxAbsoluteRelevance, currentAbsoluteRelevance);
        }
        return maxAbsoluteRelevance;
    }
}
