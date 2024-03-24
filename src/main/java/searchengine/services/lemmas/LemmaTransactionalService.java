package searchengine.services.lemmas;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.PageStatus;
import searchengine.model.SearchIndex;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.services.indexing.IndexingService;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Service
@Log4j2
@RequiredArgsConstructor
public class LemmaTransactionalService {
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final DuplicateFixService duplicateFixService;
    @Transactional
    public void saveLemmasAndIndexes(
            Page page,
            Set<Lemma> localTempLemmas,
            Set<SearchIndex> localTempIndexes) {
        lemmaRepository.saveAll(localTempLemmas);
        indexRepository.saveAll(localTempIndexes);
        page.setPageStatus(PageStatus.INDEXED);
        pageRepository.save(page);
        log.info(page.getPath() + " PAGE INDEXED!\n\tLemmas saved : " + localTempLemmas.stream().map(Lemma::getLemma).toList());
    }
    @Transactional
    public void processLemmas(Page page, HashMap<String, Integer> lemmas2ranking, Set<Lemma> localTempLemmas, Set<SearchIndex> localTempIndexes, boolean ignoreIndexingStatus) {
        for (String stringLemma : lemmas2ranking.keySet()) {
            boolean isIndexing = IndexingService.isIndexing();
            if (!isIndexing && !ignoreIndexingStatus) {
                break;
            }
            List<Lemma> lemmaModelList = lemmaRepository.findAllByLemma(stringLemma);
            Lemma lemma;
            if (!lemmaModelList.isEmpty()) {
                if(lemmaModelList.size() > 1) {
                    duplicateFixService.mergeLemmaDuplicates(lemmaModelList.get(0).getLemma());
                }
                lemma = lemmaRepository.findById(lemmaModelList.get(0).getId()).orElse(null);
                if (lemma == null) {
                    continue;
                }
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.save(lemma);
            } else {
                lemma = new Lemma(page.getSite(), stringLemma, 1);
                localTempLemmas.add(lemma);
                localTempIndexes.add(new SearchIndex(page, lemma, lemmas2ranking.get(stringLemma)));
            }
        }
    }
}
