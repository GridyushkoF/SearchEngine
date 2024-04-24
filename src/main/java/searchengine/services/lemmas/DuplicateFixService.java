package searchengine.services.lemmas;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.SearchIndex;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SearchIndexRepository;

import java.util.List;
@Service
@RequiredArgsConstructor
public class DuplicateFixService {
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void mergeAllDuplicates() {
        List<String> lemmaStringList = lemmaRepository.findAllDoubleLemmasStringList();
        lemmaStringList.forEach(this::mergeLemmaDuplicates);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void mergeLemmaDuplicates(String lemma) {
        List<SearchIndex> indexList = indexRepository.findAllByLemmaString(lemma);
        indexRepository.deleteAll(indexList);
        List<Lemma> lemmaModelList = lemmaRepository.findAllByLemma(lemma);
        Lemma firstLemmaModel = lemmaModelList.get(0);
        int resultFrequency = mergeFrequencies(lemmaModelList);
        firstLemmaModel.setFrequency(firstLemmaModel.getFrequency() + resultFrequency);
        lemmaRepository.save(firstLemmaModel);
        indexList.forEach(index -> {
            SearchIndex newIndex = new SearchIndex(index.getPage(), firstLemmaModel, index.getRanking());
            indexRepository.save(newIndex);
        });
    }
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public int mergeFrequencies(List<Lemma> lemmaModelList) {
        return lemmaModelList.stream()
                .skip(1)
                .peek(lemmaRepository::delete)
                .mapToInt(Lemma::getFrequency)
                .sum();
    }
}
