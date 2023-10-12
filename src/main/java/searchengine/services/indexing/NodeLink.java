package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.util.*;
@Getter
@RequiredArgsConstructor
@ToString(of = {"link"})
@Log4j
public class NodeLink {
    private final String link;
    private final String rootLink;
    private final Set<NodeLink> children = new HashSet<>();
    private void initChildren() {
        try {
            Document doc = Jsoup.connect(link).get();
           Elements elements = doc.select("a");
            for (Element element : elements) {
                String href = element.attr("href");
                href = href.replaceAll(" ", "").replaceAll("//","/").replaceAll("https:/","https://").replaceAll("http:/","http://");
                if (isNormalLink(href)) {
                    NodeLink childNodeLink = new NodeLink((href.startsWith("/") ? (rootLink + href) : href), rootLink);
                    String childLink = childNodeLink.getLink();
                    try {
                        if (compareHosts(childLink,link)
                                &&
                                children.stream().noneMatch(child -> child.getLink().equals(childLink)))
                        {
                            children.add(childNodeLink);
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing child node link: " + href);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing children links: " + e.getMessage());
        }
    }

    // Метод для проверки, является ли ссылка ссылкой на изображение
    private boolean isMediaLink(String link) {
        if (link.endsWith(".html") || link.endsWith("/")) {
            return false;
        }
        return  (equalsBySettings(link,List.of(".png",".jpeg",".svg",".jpg",".pdf",".doc"),"endsWith"));

    }
    public String getHost (String linkToSite) throws Exception {
                URI url = new URI(linkToSite);
                if (!linkToSite.isEmpty())
                {
                    String result =  url.isAbsolute() && url.getHost() != null ? url.getHost().replaceAll("www\\.","").toLowerCase() : "";
                    if (result.isEmpty()) {throw new Exception ("getHost () -> result is empty"); }
                    return result;
                }
        throw new Exception("Link is empty");
    }
    private boolean compareHosts (String link1, String link2) throws Exception
    {
        return getHost(link1).equals(getHost(link2));
    }
    public Set<NodeLink> getChildren()
    {
        initChildren();
        return children;
    }
    public Connection.Response getResponse() throws IOException {
        return Jsoup.connect(link).execute();
    }
    private boolean isNormalLink (String link)
    {
        List<String> prefixes = List.of("tel","mailto:","javascript","whatsapp:/");
        return !equalsBySettings(link,prefixes,"startsWith") && !link.isEmpty() && !isMediaLink(link) && !equalsBySettings(link,List.of("?","#"),"contains");
    }
    private boolean equalsBySettings (String link, List<String> prefixes, String mode)
    {
        switch (mode) {
            case "startsWith" -> {
                for (String prefix : prefixes) {
                    if (link.startsWith(prefix)) {
                        return true;
                    }
                }
            }
            case "endsWith" -> {
                for (String prefix : prefixes) {
                    if (link.endsWith(prefix)) {
                        return true;
                    }
                }
            }
            case "contains" -> {
                for (String prefix : prefixes) {
                    if (link.contains(prefix)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}