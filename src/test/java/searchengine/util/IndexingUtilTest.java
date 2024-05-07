package searchengine.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.config.YamlParser;

import java.util.List;

public class IndexingUtilTest {

    @Test
    void testGetResponse() {
        //not need
    }

    @Test
    @DisplayName("isAppropriateLink should return false for mailto links")
    void testIsAppropriateLink_mail_false() {
        String testLink = "mailto:black@gmail.com";
        Assertions.assertFalse(IndexingUtil.isAppropriateLink(testLink));
    }

    @Test
    @DisplayName("isAppropriateLink should return false for tel links")
    void testIsAppropriateLink_tel_false() {
        String testLink = "tel:+74245154562";
        Assertions.assertFalse(IndexingUtil.isAppropriateLink(testLink));
    }

    @Test
    @DisplayName("isAppropriateLink should return false for javascript links")
    void testIsAppropriateLink_js_false() {
        String testLink = "javascript:alert('Привет, мир!');";
        Assertions.assertFalse(IndexingUtil.isAppropriateLink(testLink));
    }

    @Test
    @DisplayName("isAppropriateLink should return false for whatsapp links")
    void testIsAppropriateLink_whatsapp_false() {
        String testLink = "whatsapp:/315135";
        Assertions.assertFalse(IndexingUtil.isAppropriateLink(testLink));
    }

    @Test
    @DisplayName("isAppropriateLink should return false for empty links")
    void testIsAppropriateLink_empty_false() {
        String testLink = "";
        Assertions.assertFalse(IndexingUtil.isAppropriateLink(testLink));
    }

    @Test
    @DisplayName("isAppropriateLink should return false for media file links")
    void testIsAppropriateLink_media_false() {
        String testLink = "http://somepictest.JPG/";
        Assertions.assertFalse(IndexingUtil.isAppropriateLink(testLink));
    }

    @Test
    @DisplayName("isAppropriateLink should return false for links with query parameters")
    void testIsAppropriateLink_withPathParams_false() {
        String testLink = "http://somesite?a=1";
        Assertions.assertFalse(IndexingUtil.isAppropriateLink(testLink));
    }

    @Test
    @DisplayName("isAppropriateLink should return false for links with anchors")
    void testIsAppropriateLink_withAnchor_false() {
        String testLink = "http://somesite#work";
        Assertions.assertFalse(IndexingUtil.isAppropriateLink(testLink));
    }

    @Test
    @DisplayName("isAppropriateLink should return false for links with incorrect protocols")
    void testIsAppropriateLink_incorrectProtocol_false() {
        String testLink = "bimBamBum://somesite";
        Assertions.assertFalse(IndexingUtil.isAppropriateLink(testLink));
    }

    @Test
    @DisplayName("isAppropriateLink should return true for correct http and https links")
    void testIsAppropriateLink_correct_true() {
        String testLink = "https://somesite.com";
        Assertions.assertTrue(IndexingUtil.isAppropriateLink(testLink));
    }


    @Test
    @DisplayName("notMediaLink should return false for links ending with media file extensions")
    void testNotMediaLink_media_false() {
        String testLink = "http://somepictest.JPG/";
        Assertions.assertFalse(IndexingUtil.notMediaLink(testLink));
    }

    @Test
    @DisplayName("getHost should extract the host without www from a given URL")
    void testGetHost() throws Exception {
        String testLink = "https://www.youtube.com";
        String expected = "youtube.com";
        Assertions.assertEquals(expected, IndexingUtil.getHost(testLink));
    }

    @Test
    @DisplayName("compareHosts should return true for links with the same host")
    void testCompareHosts_similarLinks_true() throws Exception {
        String link1 = "http://localhost/main";
        String link2 = "http://localhost/picture";
        Assertions.assertTrue(IndexingUtil.compareHosts(link1,link2));
    }

    @Test
    @DisplayName("compareHosts should return false for links with different hosts")
    void testCompareHosts_differentLinks_false() throws Exception {
        String link1 = "http://localhost/main";
        String link2 = "http://swag/normalization";
        Assertions.assertFalse(IndexingUtil.compareHosts(link1,link2));
    }

    @Test
    @DisplayName("getPathOf should return the path of a given URL")
    void testGetPathOf() {
        String testLink = "http://localhost/main";
        String expected = "/main";
        Assertions.assertEquals(expected,IndexingUtil.getPathOf(testLink));
    }

    @Test
    @DisplayName("getTitleOf should extract the title from the HTML content")
    void testGetTitleOf() {
        String expected = "Сайт со звуками, где их можно скачать бесплатно — ZvukiPro";
        String html = "<html><head><title>Сайт со звуками, где их можно скачать бесплатно — ZvukiPro</title></head><body></body></html>";
        Assertions.assertEquals(expected,IndexingUtil.getTitleOf(html));
    }

    @Test
    @DisplayName("equalsBySettings should return true when the link starts with a given prefix")
    void testEqualsBySettings_startsWith_true() {
        String link = "mailto:/dfdf@fdgdf.com";
        List<String> settings = List.of("mailto");
        Assertions.assertTrue(IndexingUtil.equalsBySettings(link,settings,"startsWith"));
    }

    @Test
    @DisplayName("equalsBySettings should return false when the link does not start with a given prefix")
    void testEqualsBySettings_startsWith_false() {
        String link = "oops";
        List<String> settings = List.of("mailto");
        Assertions.assertFalse(IndexingUtil.equalsBySettings(link,settings,"startsWith"));
    }

    @Test
    @DisplayName("equalsBySettings should return true when the link contains any of the given substrings")
    void testEqualsBySettings_contains_true() {
        String link = "https://www.youtube.com";
        List<String> settings = List.of("youtube","www");
        Assertions.assertTrue(IndexingUtil.equalsBySettings(link,settings,"contains"));
    }
    @Test
    @DisplayName("equalsBySettings should return false when the link does not contain any of the given substrings")
    void testEqualsBySettings_contains_false() {
        String link = "https://www.youtube.com";
        List<String> settings = List.of("l;'l;';l'","fdsgdfg");
        Assertions.assertFalse(IndexingUtil.equalsBySettings(link,settings,"contains"));
    }

    @Test
    @DisplayName("equalsBySettings should return true when the link ends with a given suffix")
    void testEqualsBySettings_endsWith_true() {
        String link = "https://www.youtube.com";
        List<String> settings = List.of(".com");
        Assertions.assertTrue(IndexingUtil.equalsBySettings(link,settings,"endsWith"));
    }
    @Test
    @DisplayName("equalsBySettings should return false when the link does not end with a given suffix")
    void testEqualsBySettings_endsWith_false() {
        String link = "https://www.youtube.com";
        List<String> settings = List.of("ghjghjgh");
        Assertions.assertFalse(IndexingUtil.equalsBySettings(link,settings,"endsWith"));
    }

    @Test
    void testGetSiteUrlByPageUrl() throws Exception{
        YamlParser yamlParser = new YamlParser();
        String srcLink = yamlParser.getSitesFromYaml().get(0).getUrl();
        String srcUrl2 = srcLink  + "/product/1";
        String siteUrl = IndexingUtil.getSiteUrlByPageUrl(srcUrl2);
        Assertions.assertNotNull(siteUrl);
    }
}
