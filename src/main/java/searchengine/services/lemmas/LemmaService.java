package searchengine.services.lemmas;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.others.IndexingTempData;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.PageStatus;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.util.LemmasExtractorUtil;
import searchengine.util.LemmasValidatorUtil;
import searchengine.util.LogMarkersUtil;

import java.text.MessageFormat;
import java.util.*;

@Service
@Log4j2
public class LemmaService {
    @Autowired
    public LemmaService(LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        try {
            LuceneMorphology russianMorphology = new RussianLuceneMorphology();
            validator = new LemmasValidatorUtil(russianMorphology);
            extractor = new LemmasExtractorUtil();
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
    private final IndexRepository indexRepository;
    private LemmaService lemmaServiceProxy;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void getAndSaveLemmasAndIndexes(PageEntity page, boolean ignoreIndexingStatus) {
        System.out.println("pageToIndex: " + page);
        if (!validator.shouldIndexPage(page, ignoreIndexingStatus)) {
            log.error(LogMarkersUtil.EXCEPTIONS, "page will not indexed( : " + page);
            return;
        }
        String htmlWithoutTags = extractor
                .removeHtmlTagsAndNormalize(page.getContent());
        HashMap<String, Integer> lemmas2ranking =
                extractor.getLemmas2RankingFromText(htmlWithoutTags);


        Set<LemmaEntity> localTempLemmas = new HashSet<>();
        Set<IndexEntity> localTempIndexes = new HashSet<>();

        long startTime = System.currentTimeMillis();
        log.info(LogMarkersUtil.INFO, "START OF LEMMAS SAVING!: " + page.getPath());
        lemmaServiceProxy.fillTempLemmasAndIndexes(page, lemmas2ranking, new IndexingTempData(localTempLemmas, localTempIndexes));

        long endTime = System.currentTimeMillis();
        log.info(LogMarkersUtil.INFO, MessageFormat.format("Page: {0}       || indexed of: {1}ms", page.getPath(), endTime - startTime));

        if (validator.shouldSaveIndexes(page, ignoreIndexingStatus, localTempLemmas, localTempIndexes)) {
            lemmaServiceProxy.saveLemmasAndIndexes(page, new IndexingTempData(localTempLemmas, localTempIndexes));
        } else {
            log.error("page will not indexed( (repeat) :" + page);
        }

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public void saveLemmasAndIndexes(
            PageEntity page,
            IndexingTempData indexingTempData) {
        lemmaRepository.saveAll(indexingTempData.tempLemmas());
        indexRepository.saveAll(indexingTempData.tempIndexes());
        page.setPageStatus(PageStatus.INDEXED);
        log.info(page.getPath() + " PAGE INDEXED!\n\tLemmas saved : " + indexingTempData.tempLemmas().stream().map(LemmaEntity::getLemma).toList());
    }

    @Transactional(propagation = Propagation.MANDATORY, isolation = Isolation.READ_COMMITTED)
    public void fillTempLemmasAndIndexes(PageEntity page, HashMap<String, Integer> lemmas2ranking, IndexingTempData indexingTempData) {
        for (String lemma : lemmas2ranking.keySet()) {
            LemmaEntity lemmaEntity = lemmaServiceProxy.updateOrCreateLemmaAndGet(page, lemma);
            indexingTempData.tempLemmas().add(lemmaEntity);
            indexingTempData.tempIndexes().add(new IndexEntity(page, lemmaEntity, lemmas2ranking.getOrDefault(lemma, 0)));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public LemmaEntity updateOrCreateLemmaAndGet(PageEntity page, String lemma) {
        Optional<LemmaEntity> lemmaEntityOptional = lemmaServiceProxy.findLemmaEntity(lemma);
        LemmaEntity result;
        if (lemmaEntityOptional.isPresent()) {
            LemmaEntity lemmaEntity = lemmaEntityOptional.get();
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            result = lemmaEntity;
        } else {
            result = new LemmaEntity(page.getSite(), lemma, 1);
        }
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Optional<LemmaEntity> findLemmaEntity(String lemma) {
        List<LemmaEntity> modelLemmas = lemmaRepository.findAllByLemma(lemma);
        if (!modelLemmas.isEmpty()) {
            return Optional.of(modelLemmas.get(0));
        }
        return Optional.empty();
    }

    @Autowired
    public void setLemmaServiceProxy(@Lazy LemmaService lemmaServiceProxy) {
        this.lemmaServiceProxy = lemmaServiceProxy;
    }

}
