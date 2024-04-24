package searchengine.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;

import java.net.URI;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class IndexingUtil {
    private static final YamlParser yamlParser = new YamlParser();
    public static Connection.Response getResponse(String link) {
        try {
            if(isAppropriateLink(link)) {
                return Jsoup.connect(link).execute();
            }
        } catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS,"Can`t get response of link: " + link + "   " + e.getMessage());
            if(e.getMessage().contains("timed out")) {
                log.error(LogMarkersUtil.EXCEPTIONS,"Trying again to get response of link: " + link + " because error = " + e.getMessage());
                return IndexingUtil.getResponse(link);
            }
        }
        return null;
    }

    public static boolean isAppropriateLink(String link) {
        List<String> blackList = List.of("tel", "mailto:", "javascript", "whatsapp:/");
        return !equalsBySettings(link, blackList, "startsWith")
                &&
                !link.isEmpty()
                &&
                notMediaLink(link)
                &&
                !equalsBySettings(link, List.of("?", "#"), "contains")
                &&
                equalsBySettings(link, List.of("http://", "https://", "ftp://", "/"), "startsWith");
    }

    public static boolean notMediaLink(String link) {
        return link.endsWith(".html") && link.endsWith("/")
                || !equalsBySettings(
                        link.endsWith("/") ? link.substring(0,link.length() - 1) : link,
                List.of(".doc",".docx",".png",".jpg",".jpeg",".pdf",".pptx",".ppt"),
                "endsWith");

    }

    public static String getHost(String linkToSite) throws Exception {
        URI url = new URI(linkToSite);
        if (!linkToSite.isEmpty()) {
            String result = url.isAbsolute() && url.getHost() != null ? url.getHost().replaceAll("www\\.", "").toLowerCase().strip() : "";
            if (result.isEmpty()) {
                throw new Exception("getHost () -> result is empty");
            }
            return result;
        }
        throw new Exception("Link is empty");
    }

    public static boolean compareHosts(String link1, String link2) throws Exception {
        return getHost(link1).equals(getHost(link2));
    }

    public static String getPathOf(String link) {
        try {
            return new URI(link).getPath();
        } catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS,"Can`t get path of link: " + link, e);
        }
        return "";
    }

    public static String getTitleOf(String textContent) {
        return Jsoup.parse(textContent).title();
    }

    public static boolean equalsBySettings(String link, List<String> settings, String mode) {
        switch (mode) {
            case "startsWith" -> {
                for (String prefix : settings) {
                    if (link.startsWith(prefix)) {
                        return true;
                    }
                }
            }
            case "endsWith" -> {
                for (String prefix : settings) {
                    if (link.endsWith(prefix)) {
                        return link.endsWith(prefix);
                    }
                }
            }
            case "contains" -> {
                for (String prefix : settings) {
                    if (link.contains(prefix)) {
                        return link.contains(prefix);
                    }
                }
            }
        }
        return false;
    }
    public static String getSiteUrlByPageUrl(String url) throws Exception {
        boolean isPageInSitesRange = false;
        String rootSiteUrl = "";
        for (ConfigSite configSite : yamlParser.getSitesFromYaml()) {
            if (IndexingUtil.compareHosts(url, configSite.getUrl())) {
                isPageInSitesRange = true;
                rootSiteUrl = configSite.getUrl();
                break;
            }
        }
        return isPageInSitesRange ? rootSiteUrl : null;
    }

}
