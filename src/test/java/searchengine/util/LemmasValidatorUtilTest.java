package searchengine.util;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LemmasValidatorUtilTest {
    private LemmasValidatorUtil lemmasValidatorUtil;
    @BeforeEach
    void setUp () throws Exception {
        LuceneMorphology russianMorphology = new RussianLuceneMorphology();
        lemmasValidatorUtil = new LemmasValidatorUtil(russianMorphology);
    }
    @Test
    void testIsCyrillic_true() {
        String string = "русский";
        Assertions.assertTrue(lemmasValidatorUtil.isCyrillic(string));
    }

    @Test
    void testIsCyrillic_false() {
        String string = "english";
        Assertions.assertFalse(lemmasValidatorUtil.isCyrillic(string));
    }


    @Test
    void testIsNotFunctional_true() {
        String functional = "дом";
        Assertions.assertTrue(lemmasValidatorUtil.isNotFunctional(functional));
    }

    @Test
    void testIsNotFunctional_false() {
        String functional = "в";
        Assertions.assertFalse(lemmasValidatorUtil.isNotFunctional(functional));
    }

    @Test
    void testShouldIndexPage() {
        //not need tests
    }

    @Test
    void testIsPageIndexingNow() {
        //not need tests
    }
}