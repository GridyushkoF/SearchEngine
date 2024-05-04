package search_engine.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.util.SnippetExtractorUtil;

import java.util.List;
import java.util.Set;

public class SnippetExtractorUtilTest {
    private SnippetExtractorUtil snippetExtractor;
    @BeforeEach
    public void setUp() {
        snippetExtractor = new SnippetExtractorUtil();
    }
    @Test
    @DisplayName("get content words with bold lemmas and words contains symbols")
    public void testGetContentWordListWithBoldLemmas_withSymbols() {
        Set<String> lemmas = Set.of("кот","черный","бежать");
        List<String> words = List.of("кот-черный","бежал");
        List<String> actual = snippetExtractor.getContentWordListWithBoldLemmas(lemmas,words);
        List<String> expected = List.of("<b>кот-черный</b>","<b>бежал</b>");
        Assertions.assertEquals(expected, actual);
    }
    @Test
    @DisplayName("get content words with bold lemmas and words without symbols")
    public void testGetContentWordListWithBoldLemmas_withoutSymbols() {
        Set<String> lemmas = Set.of("бокал","вино");
        List<String> words = List.of("бокал","красного","вина");
        List<String> actual = snippetExtractor.getContentWordListWithBoldLemmas(lemmas,words);
        List<String> expected = List.of("<b>бокал</b>","красного","<b>вина</b>");
        Assertions.assertEquals(expected, actual);
    }
}
