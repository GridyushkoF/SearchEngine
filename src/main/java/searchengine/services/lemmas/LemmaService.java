package searchengine.services.lemmas;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.services.indexing.IndexingService;
import searchengine.util.LogMarkers;

import java.text.MessageFormat;
import java.util.*;

@Service
@Log4j2
public class LemmaService {
    @Autowired
    public LemmaService
            (
            LemmaRepository lemmaRepository,
            SearchIndexRepository indexRepository,
            PageRepository pageRepository,
            DuplicateFixService duplicateFixService
            )
    {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.duplicateFixService = duplicateFixService;

        try {
            LuceneMorphology russianMorphology = new RussianLuceneMorphology();
            validator = new LemmaValidator(russianMorphology);
            extractor = new LemmaExtractor(validator,russianMorphology);
        } catch (Exception e) {
            log.error("can`t init LuceneMorphology", e);
        }
    }
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final DuplicateFixService duplicateFixService;
    private LemmaValidator validator;
    private LemmaExtractor extractor;

    public LemmaValidator getValidator() {
        return validator;
    }

    public LemmaExtractor getExtractor() {
        return extractor;
    }

    @Transactional
    public void getAndSaveLemmasAndIndexes(Page page, boolean ignoreIndexingStatus) {
        if (!validator.shouldIndexPage(page, ignoreIndexingStatus)) {
            return;
        }

        String HTMLWithoutTags = extractor
                .removeHtmlTagsAndNormalize(page.getContent());
        HashMap<String, Integer> lemmas2count =
                extractor.getLemmas2RankingFromText(HTMLWithoutTags);
        long startTime = System.currentTimeMillis();
        log.info(LogMarkers.INFO,"START OF INDEXING PAGE: " + page.getPath());

        Set<Lemma> localTempLemmas = new HashSet<>();
        Set<SearchIndex> localTempIndexes = new HashSet<>();

        processLemmas(page, lemmas2count, localTempLemmas, localTempIndexes, ignoreIndexingStatus);

        long endTime = System.currentTimeMillis() - startTime;
        log.info(LogMarkers.INFO, MessageFormat.format("Page: {0} indexed of: {1}",page.getPath(),endTime - startTime));

        if (validator.shouldSaveIndexes(page, ignoreIndexingStatus, localTempLemmas, localTempIndexes)) {
            saveLemmasAndIndexes(page, localTempLemmas, localTempIndexes);
        }
    }
    public void processLemmas(Page page, HashMap<String, Integer> lemmas2count, Set<Lemma> localTempLemmas, Set<SearchIndex> localTempIndexes, boolean ignoreIndexingStatus) {
        for (String lemmaKey : lemmas2count.keySet()) {
            boolean isIndexing = IndexingService.isIndexing();
            if (isIndexing || ignoreIndexingStatus) {
                List<Lemma> lemmaList = lemmaRepository.findAllByLemma(lemmaKey);
                Lemma lemma;
                if (!lemmaList.isEmpty()) {
                    if(lemmaList.size() > 1) {
                        duplicateFixService.mergeAllDuplicates();
                    }
                    lemma = lemmaList.get(0);
                    lemma.setFrequency(lemma.getFrequency() + 1);
                } else {
                    lemma = new Lemma(page.getSite(), lemmaKey, 1);
                }
                localTempLemmas.add(lemma);
                localTempIndexes.add(new SearchIndex(page, lemma, lemmas2count.get(lemmaKey)));
            } else {
                break;
            }
        }
    }
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
}
