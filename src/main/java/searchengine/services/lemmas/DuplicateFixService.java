package searchengine.services.lemmas;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.util.List;
@Service
@RequiredArgsConstructor
public class DuplicateFixService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void mergeAllDuplicates() {
        List<String> lemmaStringList = lemmaRepository.findAllDoubleLemmasStringList();
        lemmaStringList.forEach(this::mergeLemmaDuplicates);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void mergeLemmaDuplicates(String lemma) {
        List<IndexEntity> indexList = indexRepository.findAllByLemma(lemma);
        indexRepository.deleteAll(indexList);
        List<LemmaEntity> lemmaEntityList = lemmaRepository.findAllByLemma(lemma);
        LemmaEntity firstLemmaEntity = lemmaEntityList.get(0);
        int resultFrequency = mergeFrequencies(lemmaEntityList);
        firstLemmaEntity.setFrequency(firstLemmaEntity.getFrequency() + resultFrequency);
        lemmaRepository.save(firstLemmaEntity);
        indexList.forEach(index -> {
            IndexEntity newIndex = new IndexEntity(index.getPage(), firstLemmaEntity, index.getRanking());
            indexRepository.save(newIndex);
        });
    }
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public int mergeFrequencies(List<LemmaEntity> lemmaEntityList) {
        return lemmaEntityList.stream()
                .skip(1)
                .peek(lemmaRepository::delete)
                .mapToInt(LemmaEntity::getFrequency)
                .sum();
    }
}
