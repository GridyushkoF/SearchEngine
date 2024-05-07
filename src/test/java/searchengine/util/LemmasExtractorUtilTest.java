package searchengine.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class LemmasExtractorUtilTest {

    public LemmasExtractorUtilTest() {
    }
    private final LemmasExtractorUtil extractor = new LemmasExtractorUtil();
    @Test
    @DisplayName("get word normal form")
    public void testGetWordNormalFormTest() {
        String word1 = "цвета.";
        String expected1 = "цвет";
        String actual1 = extractor.getWordNormalForm(word1);
        String word2 = "12,";
        String expected2 = "12";
        String actual2 = extractor.getWordNormalForm(word2);
        String word3 = "заусенцы,";
        String expected3 = "заусенец";
        String actual3 = extractor.getWordNormalForm(word3);
        Assertions.assertEquals(expected1,actual1);
        Assertions.assertEquals(expected2,actual2);
        Assertions.assertEquals(expected3,actual3);

    }
    @Test
    @DisplayName("get lemmas2ranking")
    public void testGetLemmas2rankingTest() throws Exception {

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://www.playback.ru/catalog/1310.html/")
                .build();
        Response response = okHttpClient.newCall(request).execute();
        String html = response.body().string();
        HashMap<String, Integer> lemmas2ranking = extractor.getLemmas2RankingFromText(extractor.removeHtmlTagsAndNormalize(html));
        System.out.println(lemmas2ranking);
        Assertions.assertTrue(lemmas2ranking.get("чехол") > 1);
    }
    @Test
    @DisplayName("split word by symbols")
    public void testSplitWordBySymbolsTest() {
        List<String> actual =  extractor.splitWordBySymbols("java-разработчик.с-нуля");
        List<String> expected = List.of("java","разработчик","с","нуля");
        Assertions.assertEquals(expected, actual);
    }
    @Test
    @DisplayName("split text on words")
    public void testSplitTextOnWordsTest() {
        List<String> actual =  extractor.splitTextOnWords("java-разработчик.с-нуля купить курсы python-django 1000.р");
        List<String> expected = List.of("java","разработчик","с","нуля","купить","курсы","python","django","1000","р");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("unique words from text getting")
    void testGetUniqueLemmasFromText() {
        String testString = "это тестовый-текст текст";
        Set<String> expected = Set.of("этот","тестовый","текст");
        Set<String> actual = extractor.getUniqueLemmasFromText(testString);
        System.out.println(actual);
        Assertions.assertTrue(expected.containsAll(actual));
    }

    @Test
    @DisplayName("not unique words from text getting")
    void testGetNotUniqueStringLemmasFromText() {
        String testString = "это тестовый-текст текст";
        List<String> expected = List.of("этот","тестовый","текст","текст");
        Set<String> actual = extractor.getUniqueLemmasFromText(testString);
        Assertions.assertTrue(expected.containsAll(actual));
    }

    @Test
    @DisplayName("merging lemmas list 2 text")
    void testMergeLemmasToText() {
        List<String> lemmas = List.of("lemma1", "lemma2", "lemma3");
        String expected = "lemma1 lemma2 lemma3";
        String actual = extractor.mergeLemmasToText(lemmas);
        Assertions.assertEquals(expected,actual);
    }
}
