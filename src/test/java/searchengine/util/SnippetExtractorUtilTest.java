package searchengine.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.model.PageEntity;
import searchengine.model.PageStatus;
import searchengine.model.SiteEntity;

import java.io.IOException;
import java.util.ArrayList;
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
        Set<String> lemmas = Set.of("кот", "черный", "бежать");
        List<String> words = List.of("кот-черный", "бежал");
        List<String> actual = snippetExtractor.getContentWordListWithBoldLemmas(lemmas, words);
        List<String> expected = List.of("<b>кот-черный</b>", "<b>бежал</b>");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("get content words with bold lemmas and words without symbols")
    public void testGetContentWordListWithBoldLemmas_withoutSymbols() {
        Set<String> lemmas = Set.of("бокал", "вино");
        List<String> words = List.of("бокал", "красного", "вина");
        List<String> actual = snippetExtractor.getContentWordListWithBoldLemmas(lemmas, words);
        List<String> expected = List.of("<b>бокал</b>", "красного", "<b>вина</b>");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("get html snippet")
    void testGetHtmlSnippet() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://www.playback.ru/catalog/940.html")
                .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            String htmlContent = response.body().string();
            PageEntity pageEntity = new PageEntity(new SiteEntity(),"/catalog/940.html",200,htmlContent, PageStatus.INDEXED);
            String actual = snippetExtractor.getHtmlSnippet(pageEntity,Set.of("внешний","аккумулятор","remax"));
            String expected = "Черный 2200р. Купить <b>Внешний</b> <b>аккумулятор</b> Hoco J96 Strider, 5000mAh, черный 1300р. Купить <b>Внешний</b> <b>аккумулятор</b> <b>Remax</b> RPP-158, 10000 mAh 2400р. Купить <b>Внешний</b> <b>аккумулятор</b> <b>Remax</b> RPP-533, 10000 mAh...";
            Assertions.assertEquals(expected,actual);
        } catch (IOException e) {
            throw new RuntimeException("cant send http");
        }
        System.out.println(request);
    }

    @Test
    @DisplayName("find longest bold lemmas row")
    void testFindLongestBoldLemmasRow() {
        List<String> contentWordsWithBoldLemmas = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            if (i == 0) {
                contentWordsWithBoldLemmas.addAll(List.of("<b>это</b>","<b>короткая</b>","цепочка","прервалась("));
            } else if (i == 60) {
                contentWordsWithBoldLemmas.addAll(List.of("<b>это</b>","<b>длинная</b>","<b>цепочка</b>"));
            } else {
                contentWordsWithBoldLemmas.add(String.valueOf(i));
            }
        }
        List<Integer> expected = List.of(63,64,65);
        List<Integer> actual = snippetExtractor.findLongestBoldLemmasRow(contentWordsWithBoldLemmas);
        Assertions.assertEquals(expected, actual);
    }
}
