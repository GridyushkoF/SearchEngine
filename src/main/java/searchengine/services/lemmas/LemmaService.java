package searchengine.services.lemmas;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.others.IndexingTempData;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.PageStatus;
import searchengine.model.SearchIndex;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.util.LemmaExtractorCacheProxy;
import searchengine.util.LemmasExtractorUtil;
import searchengine.util.LemmasValidatorUtil;
import searchengine.util.LogMarkersUtil;

import java.sql.SQLDataException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Log4j2
public class LemmaService {
    @Autowired
    public LemmaService(LemmaRepository lemmaRepository, SearchIndexRepository indexRepository, PageRepository pageRepository, DuplicateFixService duplicateFixService) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.duplicateFixService = duplicateFixService;
        try {
            LuceneMorphology russianMorphology = new RussianLuceneMorphology();
            validator = new LemmasValidatorUtil(russianMorphology);

            extractor = new LemmasExtractorUtil(new LemmaExtractorCacheProxy());
        } catch (Exception e) {
            log.error("can`t init LuceneMorphology", e);
        }
    }

    private LemmasValidatorUtil validator;
    private LemmasExtractorUtil extractor;

    public LemmasExtractorUtil getExtractor() {
        return extractor;
    }

    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final DuplicateFixService duplicateFixService;
    private LemmaService lemmaServiceProxy;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void getAndSaveLemmasAndIndexes(Page page, boolean ignoreIndexingStatus) {
        if (!validator.shouldIndexPage(page, ignoreIndexingStatus)) {
            return;
        }
        String htmlWithoutTags = extractor
                .removeHtmlTagsAndNormalize(page.getContent());
        HashMap<String, Integer> lemmas2ranking =
                extractor.getLemmas2RankingFromText(htmlWithoutTags);


        Set<Lemma> localTempLemmas = new HashSet<>();
        Set<SearchIndex> localTempIndexes = new HashSet<>();

        long startTime = System.currentTimeMillis();
        log.info(LogMarkersUtil.INFO, "START OF LEMMAS SAVING!: " + page.getPath());
        lemmaServiceProxy.fillTempLemmasAndIndexesIgnoreIndexingStatus(page, lemmas2ranking, new IndexingTempData(localTempLemmas, localTempIndexes));

        long endTime = System.currentTimeMillis();
        log.info(LogMarkersUtil.INFO, MessageFormat.format("Page: {0}       || indexed of: {1}ms", page.getPath(), endTime - startTime));

        if (validator.shouldSaveIndexes(page, ignoreIndexingStatus, localTempLemmas, localTempIndexes)) {
            lemmaServiceProxy.saveLemmasAndIndexes(page, new IndexingTempData(localTempLemmas, localTempIndexes));
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(maxAttempts = 50)
    public void saveLemmasAndIndexes(
            Page page,
            IndexingTempData indexingTempData) {
        lemmaRepository.saveAll(indexingTempData.tempLemmas());
        indexRepository.saveAll(indexingTempData.tempIndexes());
        page.setPageStatus(PageStatus.INDEXED);
        pageRepository.save(page);
        log.info(page.getPath() + " PAGE INDEXED!\n\tLemmas saved : " + indexingTempData.tempLemmas().stream().map(Lemma::getLemma).toList());
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void fillTempLemmasAndIndexesIgnoreIndexingStatus(Page page, HashMap<String, Integer> lemmas2ranking, IndexingTempData indexingTempData) {
        for (String stringLemma : lemmas2ranking.keySet()) {
            try {
                Lemma lemma = lemmaServiceProxy.updateOrCreateLemmaAndGet(page, stringLemma);
                indexingTempData.tempLemmas().add(lemma);
                indexingTempData.tempIndexes().add(new SearchIndex(page, lemma, lemmas2ranking.getOrDefault(stringLemma, 0)));
            } catch (SQLDataException e) {
                log.error(e);
            }
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Lemma updateOrCreateLemmaAndGet(Page page, String stringLemma) throws SQLDataException {
        Lemma lemma = lemmaServiceProxy.findLemmaAndMergeDuplicatesIfExists(stringLemma);
        return lemma != null ? lemmaServiceProxy.incrementLemmaFrequencyAndSave(lemma) : lemmaServiceProxy.createAndSaveLemma(page, stringLemma);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Lemma findLemmaAndMergeDuplicatesIfExists(String stringLemma) {
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

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Lemma createAndSaveLemma(Page page, String stringLemma) {
        return lemmaRepository.save(new Lemma(page.getSite(), stringLemma, 1));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    @Retryable(maxAttempts = 5,retryFor = Throwable.class)
    public Lemma incrementLemmaFrequencyAndSave(Lemma lemma)  throws SQLDataException{
        try {
            lemma.setFrequency(lemma.getFrequency() + 1);
            return lemmaRepository.save(lemma);
        } catch (Throwable e) {
            log.error("Возникло исключение при попытке сохранить лемму (вероятно, deadlock), вызываем этот же метод с синхронизацией");
        }
        throw new SQLDataException();
    }

    @Recover
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public synchronized Lemma recoverIncrementLemmaFrequencyAndSave(Throwable throwable, Lemma lemma) {
        synchronized (lemmaRepository) {
            try {
                lemma.setFrequency(lemma.getFrequency() + 1);
                return lemmaRepository.save(lemma);
            } catch (Throwable e) {
                log.error("Error occurred while saving lemma: {}", e.getMessage());
                throw new RuntimeException("Failed to save lemma, recover had not helped", e);
            }
        }
    }

    @Autowired
    public void setLemmaServiceProxy(@Lazy LemmaService lemmaServiceProxy) {
        this.lemmaServiceProxy = lemmaServiceProxy;
    }

}
