package searchengine.services.lemmas;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.others.IndexingTempData;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.PageStatus;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.util.LemmasExtractorUtil;
import searchengine.util.LemmasValidatorUtil;
import searchengine.util.LogMarkersUtil;

import java.util.HashMap;
import java.util.List;

@Service
@Log4j2
@Getter
public class LemmaService {
    @Autowired
    public LemmaService(PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, LemmasFrequencyManager lemmasFrequencyManager) {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmasFrequencyManager = lemmasFrequencyManager;
        try {
            LuceneMorphology russianMorphology = new RussianLuceneMorphology();
            validator = new LemmasValidatorUtil(russianMorphology);
            extractor = new LemmasExtractorUtil();
        } catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS,"can`t init LuceneMorphology", e);
        }
    }

    private LemmasValidatorUtil validator;
    private LemmasExtractorUtil extractor;

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final LemmasFrequencyManager lemmasFrequencyManager;
    private LemmaService selfProxy;

    public void getAndSaveLemmasAndIndexes(PageEntity page) {
        selfProxy.saveTempIndexingData(selfProxy.fillTempIndexingData(page),page);
    }

    public void getAndSaveLemmasAndIndexesIfIsIndexing(PageEntity page) {
        if (validator.isPageIndexingNow(page)) {
            getAndSaveLemmasAndIndexes(page);
        } else {
            log.error(LogMarkersUtil.EXCEPTIONS,"Страница сейчас больше не индексируется и не может быть проиндексирована: " + page.getPath());
        }
    }
    @Transactional(isolation = Isolation.SERIALIZABLE,timeout = 40)
    public void saveTempIndexingData(IndexingTempData indexingTempData, PageEntity page) {
        lemmaRepository.saveAll(indexingTempData.getTempLemmas());
        indexRepository.saveAll(indexingTempData.getTempIndexes());
        log.info(LogMarkersUtil.INFO,"NEW LEMMAS SAVED: " + indexingTempData.getTempLemmas());
        page.setPageStatus(PageStatus.INDEXED);
        pageRepository.save(page);
    }
    @Transactional(isolation = Isolation.REPEATABLE_READ,timeout = 60)
    public IndexingTempData fillTempIndexingData(PageEntity page) {
        String htmlWithoutTags = extractor.removeHtmlTagsAndNormalize(page.getContent());
        HashMap<String, Integer> lemmas2ranking = extractor.getLemmas2RankingFromText(htmlWithoutTags);
        IndexingTempData indexingTempData = new IndexingTempData();
        lemmas2ranking.forEach((lemma, ranking) -> {
            LemmaEntity lemmaEntity;
            IndexEntity indexEntity = new IndexEntity(page, null, ranking);
            List<LemmaEntity> lemmaEntities = lemmaRepository.findAllByLemma(lemma);
            if (lemmaEntities.size() >= 1) {
                lemmaEntity = lemmaEntities.get(0);
                indexEntity.setLemmaEntity(lemmaEntity);
                lemmasFrequencyManager.createAndUpdateLemmaFrequency(lemma);
            } else {
                lemmaEntity = new LemmaEntity(page.getSite(), lemma, 1);
                indexingTempData.getTempLemmas().add(lemmaEntity);
                indexEntity.setLemmaEntity(lemmaEntity);
            }
            indexingTempData.getTempIndexes().add(indexEntity);
        });
        return indexingTempData;
    }

    @Autowired
    public void setSelfProxy(@Lazy LemmaService selfProxy) {
        this.selfProxy = selfProxy;
    }
}
