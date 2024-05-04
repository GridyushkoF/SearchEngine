package searchengine.services.lemmas;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.repositories.LemmaRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class LemmasFrequencyManager {
    private final LemmaRepository lemmaRepository;
    private LemmasFrequencyManager selfProxy;
    private final Map<String,Integer> lemmas2Frequency = Collections.synchronizedMap(new HashMap<>());
    public void createAndUpdateLemmaFrequency(String lemma) {
        lemmas2Frequency.put(lemma,lemmas2Frequency.getOrDefault(lemma,0) + 1);
    }
    @Transactional(timeout = -1,isolation = Isolation.READ_COMMITTED)
    public void applyAllFrequencies() {
        Set<LemmaEntity> lemmasOnSave = new HashSet<>();
        lemmas2Frequency.forEach((lemma,frequency) -> {
            List<LemmaEntity> lemmaEntityList = lemmaRepository.findAllByLemma(lemma);
            if(lemmaEntityList.size() >= 1) {
                LemmaEntity currentLemma = lemmaEntityList.get(0);
                currentLemma.setFrequency(currentLemma.getFrequency() + frequency);
                lemmasOnSave.add(currentLemma);
            }
        });
        lemmaRepository.saveAll(lemmasOnSave);
    }

    @Autowired
    public void setSelfProxy(@Lazy LemmasFrequencyManager selfProxy) {
        this.selfProxy = selfProxy;
    }
    @Scheduled(fixedDelay = 30000)
    @Transactional(timeout = -1)
    public void applyAllFrequenciesAndClearSerially() {
        log.info("-------------------------Frequencies are saving to database----------");
        selfProxy.applyAllFrequencies();
        lemmas2Frequency.clear();
        log.info("-------------------------Frequencies saved to database!----------");
    }
}
