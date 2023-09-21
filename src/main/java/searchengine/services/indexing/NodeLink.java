package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
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
public class NodeLink {
    private final String link;
    private final String rootLink;
    private final Set<NodeLink> children = new HashSet<>();
    private void initChildren() {
        try {
            Document doc = Jsoup.connect(link).ignoreContentType(true).get();
            Elements elements = doc.select("a");
            for (Element element : elements) {
                String href = element.attr("href");
                NodeLink childNodeLink = new NodeLink((href.startsWith("/") ? (rootLink + href) : href ), rootLink);
                    try {
                        if (getHost(childNodeLink.getLink()).equals(getHost(link)) && children.stream().noneMatch(child -> child.getLink().equals(childNodeLink.getLink()))) {
                            children.add(childNodeLink);
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing child node link: " + href);
                    }
            }
            System.out.println("ОТ " + link + " \n" + children);
        } catch (Exception e) {
            System.err.println("Error initializing children links: " + e.getMessage());
        }
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
    public Set<NodeLink> getChildren()
    {
        initChildren();
        return children;
    }
    public Connection.Response getResponse() throws IOException {
        return Jsoup.connect(link).ignoreContentType(true).execute();
    }
}