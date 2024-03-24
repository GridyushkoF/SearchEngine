package searchengine.services.lemmas;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.util.LemmaExtractor;
import searchengine.util.LemmaValidator;
import searchengine.util.LogMarkers;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Service
@Log4j2
public class LemmaService {
    @Autowired
    public LemmaService(LemmaTransactionalService lemmaTransactionalService)
    {
        this.lemmaTransactionalService = lemmaTransactionalService;
        try {
            LuceneMorphology russianMorphology = new RussianLuceneMorphology();
            validator = new LemmaValidator(russianMorphology);
            extractor = new LemmaExtractor(validator,russianMorphology);
        } catch (Exception e) {
            log.error("can`t init LuceneMorphology", e);
        }
    }
    private final LemmaTransactionalService lemmaTransactionalService;
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
        String htmlWithoutTags = extractor
                .removeHtmlTagsAndNormalize(page.getContent());
        HashMap<String, Integer> lemmas2ranking =
                extractor.getLemmas2RankingFromText(htmlWithoutTags);


        Set<Lemma> localTempLemmas = new HashSet<>();
        Set<SearchIndex> localTempIndexes = new HashSet<>();

        long startTime = System.currentTimeMillis();
        log.info(LogMarkers.INFO,"START OF LEMMAS SAVING!: " + page.getPath());
        lemmaTransactionalService.processLemmas(page, lemmas2ranking, localTempLemmas, localTempIndexes, ignoreIndexingStatus);

        long endTime = System.currentTimeMillis();
        log.info(LogMarkers.INFO, MessageFormat.format("Page: {0}       indexed of: {1}ms",page.getPath(),endTime - startTime));

        if (validator.shouldSaveIndexes(page, ignoreIndexingStatus, localTempLemmas, localTempIndexes)) {
            lemmaTransactionalService.saveLemmasAndIndexes(page, localTempLemmas, localTempIndexes);
        }
    }


}
