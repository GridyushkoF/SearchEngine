package searchengine.util;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
public class LemmaExtractorCacheProxy {

    private LuceneMorphology morphology;
    private LemmasValidatorUtil validator;

    public LemmaExtractorCacheProxy() {
        try {
            this.morphology = new RussianLuceneMorphology();
            validator = new LemmasValidatorUtil(morphology);
        }catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS,e.getMessage(),e);
        }

    }

    @Cacheable("wordNormalForms")
    public String getWordNormalForm(String word) {
        String wordNormalForm = word.toLowerCase();
        String symbolsRegex = LemmasValidatorUtil.SYMBOLS_REGEX;
        try {
            if(!word.matches(symbolsRegex)) {
                wordNormalForm = wordNormalForm.replaceAll(symbolsRegex,"");
            }

            if(!wordNormalForm.isEmpty() && validator.isCyrillic(wordNormalForm)) {
                List<String> wordNormalForms = morphology.getNormalForms(wordNormalForm);
                if(wordNormalForms != null) {
                    wordNormalForm  = wordNormalForms.get(0);
                }
            }
        } catch (Exception e) {

            log.error("NPE (null pointer exception): getNormalForms(word)).get(0) == null/word = " + wordNormalForm, e);
        }
        return wordNormalForm;
    }
}
