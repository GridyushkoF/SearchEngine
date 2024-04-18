package searchengine.services.lemmas;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.dto.others.IndexingTempData;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.PageStatus;
import searchengine.model.SearchIndex;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;

import java.util.HashMap;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class LemmaTransactionalProxy {
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final DuplicateFixService duplicateFixService;

    @Transactional
    public void saveLemmasAndIndexes(
            Page page,
            IndexingTempData indexingTempData) {
        lemmaRepository.saveAll(indexingTempData.tempLemmas());
        indexRepository.saveAll(indexingTempData.tempIndexes());
        page.setPageStatus(PageStatus.INDEXED);
        pageRepository.save(page);
        log.info(page.getPath() + " PAGE INDEXED!\n\tLemmas saved : " + indexingTempData.tempLemmas().stream().map(Lemma::getLemma).toList());
    }

    @Transactional
    public void fillTempLemmasAndIndexesIgnoreIndexingStatus(Page page, HashMap<String, Integer> lemmas2ranking, IndexingTempData indexingTempData) {
        for (String stringLemma : lemmas2ranking.keySet()) {
            Lemma lemma = updateOrCreateLemmaAndGet(page, stringLemma);
            indexingTempData.tempLemmas().add(lemma);
            indexingTempData.tempIndexes().add(new SearchIndex(page, lemma, lemmas2ranking.get(stringLemma)));
        }
    }

    public Lemma updateOrCreateLemmaAndGet(Page page, String stringLemma) {
        Lemma lemma = findLemmaAndMergeDuplicatesIfExists(stringLemma);
        return lemma != null ? incrementLemmaFrequencyAndSave(lemma) : createAndSaveLemma(page, stringLemma);
    }

    private Lemma findLemmaAndMergeDuplicatesIfExists(String stringLemma) {
        List<Lemma> modelLemmas = lemmaRepository.findAllByLemma(stringLemma);
        if (!modelLemmas.isEmpty()) {
            if (modelLemmas.size() > 1) {
                duplicateFixService.mergeLemmaDuplicates(stringLemma);
                return lemmaRepository.findAllByLemma(stringLemma).get(0);
            }
            return modelLemmas.get(0);
        }
        return null;
    }

    private Lemma incrementLemmaFrequencyAndSave(Lemma lemma) {
        lemma.setFrequency(lemma.getFrequency() + 1);
        return lemmaRepository.save(lemma);
    }

    private Lemma createAndSaveLemma(Page page, String stringLemma) {
        return lemmaRepository.save(new Lemma(page.getSite(), stringLemma, 1));
    }
}
