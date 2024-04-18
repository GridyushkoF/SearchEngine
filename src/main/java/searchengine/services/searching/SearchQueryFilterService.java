package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import org.jsoup.internal.StringUtil;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.util.LemmaPriority;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchQueryFilterService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private static final int MAX_LEMMA_FREQUENCY_PERCENT = 70;

    public List<Lemma> getFilteredAndSortedByFrequencyLemmas(Set<String> lemmaStringSet) {
        List<Lemma> filtredLemmaList = getFilteredLemmas(lemmaStringSet);
        filtredLemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        return filtredLemmaList;
    }

    private List<Lemma> getFilteredLemmas(Set<String> lemmaStringSet) {
        Map<LemmaPriority, List<Lemma>> priority2Lemmas = distributeLemmasToGroupsByPriority(lemmaStringSet);
        List<Lemma> highPriorityBecauseNormalFrequency = priority2Lemmas.getOrDefault(LemmaPriority.PRIORITY, new ArrayList<>());
        List<Lemma> lowPriorityBecauseTooOften = priority2Lemmas.getOrDefault(LemmaPriority.LOW_PRIORITY, new ArrayList<>());

        return filterLemmasByFrequency(highPriorityBecauseNormalFrequency, lowPriorityBecauseTooOften);
    }

    private List<Lemma> filterLemmasByFrequency(List<Lemma> highPriorityBecauseNormalFrequency, List<Lemma> lowPriorityBecauseTooOften) {
        return (highPriorityBecauseNormalFrequency.size() > lowPriorityBecauseTooOften.size()) ?
                highPriorityBecauseNormalFrequency :
                Stream.concat(lowPriorityBecauseTooOften.stream(), highPriorityBecauseNormalFrequency.stream())
                        .collect(Collectors.toList());
    }


    private Map<LemmaPriority, List<Lemma>> distributeLemmasToGroupsByPriority(Set<String> lemmas) {
        return lemmas.stream()
                .map(lemmaRepository::findByLemma)
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(getLemmaPriorityDistributor()));
    }

    private Function<Lemma, LemmaPriority> getLemmaPriorityDistributor() {
        return lemmaModel -> {
            Site siteOfLemmaModel = lemmaModel.getSite();
            long pagesAmount = pageRepository.countIndexedPagesBySite(siteOfLemmaModel);
            float percent = (float) (lemmaModel.getFrequency() * 100) / pagesAmount;
            LemmaPriority priorityWhenLemmaExceedsMaxFrequency = percent > MAX_LEMMA_FREQUENCY_PERCENT ? LemmaPriority.LOW_PRIORITY : LemmaPriority.PRIORITY;
            if (StringUtil.isNumeric(lemmaModel.getLemma())) {
                return LemmaPriority.LOW_PRIORITY;
            }
            return priorityWhenLemmaExceedsMaxFrequency;
        };
    }
}
