package searchengine.services.lemmas;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.others.IndexingTempData;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.util.LemmasExtractorUtil;
import searchengine.util.LemmaExtractorCacheProxy;
import searchengine.util.LemmasValidatorUtil;
import searchengine.util.LogMarkersUtil;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Service
@Log4j2
public class LemmaService {
    @Autowired
    public LemmaService(LemmaTransactionalProxy lemmaTransactionalProxy)
    {
        this.lemmaTransactionalProxy = lemmaTransactionalProxy;
        try {
            LuceneMorphology russianMorphology = new RussianLuceneMorphology();
            validator = new LemmasValidatorUtil(russianMorphology);
            LemmaExtractorCacheProxy extractorProxyCache = new LemmaExtractorCacheProxy();
            extractor = new LemmasExtractorUtil(extractorProxyCache);
        } catch (Exception e) {
            log.error("can`t init LuceneMorphology", e);
        }
    }
    private final LemmaTransactionalProxy lemmaTransactionalProxy;
    private LemmasValidatorUtil validator;
    private LemmasExtractorUtil extractor;
    public LemmasExtractorUtil getExtractor() {
        return extractor;
    }

    @Transactional
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
        log.info(LogMarkersUtil.INFO,"START OF LEMMAS SAVING!: " + page.getPath());
        lemmaTransactionalProxy.fillTempLemmasAndIndexesIgnoreIndexingStatus(page, lemmas2ranking, new IndexingTempData(localTempLemmas,localTempIndexes));

        long endTime = System.currentTimeMillis();
        log.info(LogMarkersUtil.INFO, MessageFormat.format("Page: {0}       indexed of: {1}ms",page.getPath(),endTime - startTime));

        if (validator.shouldSaveIndexes(page, ignoreIndexingStatus, localTempLemmas, localTempIndexes)) {
            lemmaTransactionalProxy.saveLemmasAndIndexes(page, new IndexingTempData(localTempLemmas, localTempIndexes));
        }
    }


}
