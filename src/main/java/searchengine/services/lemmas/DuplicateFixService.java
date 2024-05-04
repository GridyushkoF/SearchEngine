package searchengine.services.lemmas;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.util.LogMarkersUtil;

import java.text.MessageFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class DuplicateFixService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private DuplicateFixService selfProxy;
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mergeAllDuplicates() {
        List<String> lemmaStringList = lemmaRepository.findAllDoubleLemmasStringList();
        lemmaStringList.forEach(selfProxy::mergeLemmasAndIndexesDuplicates);
    }
    @Transactional(propagation = Propagation.REQUIRED)
    public void mergeLemmasAndIndexesDuplicates(String lemma) {
        List<IndexEntity> indexes = indexRepository.findAllByLemma(lemma);
        List<LemmaEntity> lemmas = lemmaRepository.findAllByLemma(lemma);
        LemmaEntity firstLemmaEntity = lemmas.get(0);
        int resultFrequency = mergeFrequencies(lemmas) + firstLemmaEntity.getFrequency();
        firstLemmaEntity.setFrequency(resultFrequency);
        lemmaRepository.save(firstLemmaEntity);

        indexes.forEach(index -> {
            index.setLemmaEntity(firstLemmaEntity);
        });
        indexRepository.saveAll(indexes);
        log.info(LogMarkersUtil.INFO, MessageFormat.format("Лемма -{0}- была успешно нормализована, было {1} сущностей, а стала частота {2}",lemma,lemmas.size(),resultFrequency));
    }
    @Transactional(propagation = Propagation.REQUIRED)
    public int mergeFrequencies(List<LemmaEntity> lemmaEntityList) {
        return lemmaEntityList.stream()
                .skip(1)
                .peek(lemmaRepository::delete)
                .mapToInt(LemmaEntity::getFrequency)
                .sum();
    }

    @Autowired
    public void setSelfProxy(@Lazy DuplicateFixService selfProxy) {
        this.selfProxy = selfProxy;
    }

}
