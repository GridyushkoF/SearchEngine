package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.internal.StringUtil;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.util.LemmaPriority;
import searchengine.util.LogMarkersUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Log4j2
public class SearchQueryFilterService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private static final int MAX_LEMMA_FREQUENCY_PERCENT = 70;

    public List<LemmaEntity> getFilteredAndSortedByFrequencyLemmas(Set<String> lemmaStringSet) {
        log.info(LogMarkersUtil.INFO,"filtering lemmas from query");
        List<LemmaEntity> filteredLemmaListEntity = getFilteredLemmas(lemmaStringSet);
        filteredLemmaListEntity.sort(Comparator.comparingInt(LemmaEntity::getFrequency));
        return filteredLemmaListEntity;
    }

    private List<LemmaEntity> getFilteredLemmas(Set<String> lemmaStringSet) {
        Map<LemmaPriority, List<LemmaEntity>> priority2Lemmas = distributeLemmasToGroupsByPriority(lemmaStringSet);
        List<LemmaEntity> highPriorityBecauseNormalFrequency = priority2Lemmas.getOrDefault(LemmaPriority.PRIORITY, new ArrayList<>());
        List<LemmaEntity> lowPriorityBecauseTooOften = priority2Lemmas.getOrDefault(LemmaPriority.LOW_PRIORITY, new ArrayList<>());

        return filterLemmasByFrequency(highPriorityBecauseNormalFrequency, lowPriorityBecauseTooOften);
    }

    private List<LemmaEntity> filterLemmasByFrequency(List<LemmaEntity> highPriorityBecauseNormalFrequency, List<LemmaEntity> lowPriorityBecauseTooOften) {
        return (highPriorityBecauseNormalFrequency.size() > lowPriorityBecauseTooOften.size()) ?
                highPriorityBecauseNormalFrequency :
                Stream.concat(lowPriorityBecauseTooOften.stream(), highPriorityBecauseNormalFrequency.stream())
                        .collect(Collectors.toList());
    }


    private Map<LemmaPriority, List<LemmaEntity>> distributeLemmasToGroupsByPriority(Set<String> lemmas) {
        return lemmas.stream()
                .map(lemmaRepository::findByLemma)
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(getLemmaPriorityDistributor()));
    }

    private Function<LemmaEntity, LemmaPriority> getLemmaPriorityDistributor() {
        return lemmaEntity -> {
            SiteEntity SiteOfLemmaEntity = lemmaEntity.getSite();
            long pagesAmount = pageRepository.countIndexedPagesBySite(SiteOfLemmaEntity);
            float percent = (float) (lemmaEntity.getFrequency() * 100) / pagesAmount;
            LemmaPriority priorityWhenLemmaExceedsMaxFrequency = percent > MAX_LEMMA_FREQUENCY_PERCENT ? LemmaPriority.LOW_PRIORITY : LemmaPriority.PRIORITY;
            if (StringUtil.isNumeric(lemmaEntity.getLemma())) {
                return LemmaPriority.LOW_PRIORITY;
            }
            return priorityWhenLemmaExceedsMaxFrequency;
        };
    }
}
