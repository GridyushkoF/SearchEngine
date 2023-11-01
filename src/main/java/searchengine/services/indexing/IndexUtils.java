package searchengine.services.indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class IndexUtils {
    public static Connection.Response getResponse(String link) throws IOException {
        return Jsoup.connect(link).execute();
    }
    public static boolean isNormalLink (String link)
    {
        List<String> settings = List.of("tel","mailto:","javascript","whatsapp:/");
        return !equalsBySettings(link,settings,"startsWith")
                &&
                !link.isEmpty()
                &&
                !isMediaLink(link)
                &&
                !equalsBySettings(link,List.of("?","#"),"contains")
                &&
                equalsBySettings(link,List.of("http://","https://","ftp://","/"),"startsWith");
    }
    public static boolean isMediaLink(String link) {
        if (link.endsWith(".html") || link.endsWith("/")) {
            return false;
        }
        return  (equalsBySettings(link,List.of(".png",".jpeg",".svg",".jpg",".pdf",".doc"),"endsWith"));

    }
    public static String getHost (String linkToSite) throws Exception {
        URI url = new URI(linkToSite);
        if (!linkToSite.isEmpty())
        {
            String result =  url.isAbsolute() && url.getHost() != null ? url.getHost().replaceAll("www\\.","").toLowerCase() : "";
            if (result.isEmpty()) {throw new Exception ("getHost () -> result is empty"); }
            return result;
        }
        throw new Exception("Link is empty");
    }
    public static boolean compareHosts (String link1, String link2) throws Exception
    {
        return getHost(link1).equals(getHost(link2));
    }
    public static String getPathOf(String link) throws URISyntaxException {return new URI(link).getPath();}
    public static boolean equalsBySettings (String link, List<String> settings, String mode)
    {
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
                        return true;
                    }
                }
            }
            case "contains" -> {
                for (String prefix : settings) {
                    if (link.contains(prefix)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
