package searchengine.util;

import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;

import java.net.URI;
import java.util.List;

@Log4j2
public class IndexingUtils {
    public static Connection.Response getResponse(String link) {
        try {
            return Jsoup.connect(link).execute();
        } catch (Exception e) {
            log.error(LogMarkers.EXCEPTIONS,"Can`t get response of link: " + link, e);
        }
        return null;
    }

    public static boolean isAppropriateLink(String link) {
        List<String> settings = List.of("tel", "mailto:", "javascript", "whatsapp:/");
        return !equalsBySettings(link, settings, "startsWith")
                &&
                !link.isEmpty()
                &&
                !isMediaLink(link)
                &&
                !equalsBySettings(link, List.of("?", "#"), "contains")
                &&
                equalsBySettings(link, List.of("http://", "https://", "ftp://", "/"), "startsWith");
    }

    public static boolean isMediaLink(String link) {
        if (link.endsWith(".html") || link.endsWith("/")) {
            return false;
        }
        return (equalsBySettings(link, List.of(".png", ".jpeg", ".svg", ".jpg", ".pdf", ".doc"), "endsWith"));

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
            log.error(LogMarkers.EXCEPTIONS,"Can`t get path of link: " + link, e);
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

    public static String getSiteName(String siteUrl) {
        for (ConfigSite configSite : YamlParser.getSitesFromYaml()) {
            if (siteUrl.equals(configSite.getUrl())) {
                return configSite.getName();
            }
        }
        return "Сайт не опознан!";
    }
}
