package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.services.DebugService;

import java.util.*;
@Getter
@RequiredArgsConstructor
@ToString(of = {"link"})
@Log4j
public class NodeLink {
    private final String link;
    private final String rootLink;
    private final Set<NodeLink> children = new HashSet<>();
    private static final DebugService D_S = new DebugService();
    private void initChildren() {
        D_S.markStart();
        try {
            Document doc = Jsoup.connect(link).get();
            for (Element element : doc.select("a")) {
                String link = element.attr("href");
                link = link
                        .replaceAll(" ", "")
                        .replaceAll("//","/")
                        .replaceAll("https:/","https://")
                        .replaceAll("http:/","http://");
                if (IndexUtils.isApproptiateLink(link)) {
                    String childLink = (link.startsWith("/") ? (rootLink + link) : link);
                    try {
                        if (IndexUtils.compareHosts(childLink, this.link)
                                &&
                                children.stream().noneMatch(child -> child.getLink().equals(childLink)))
                        {
                            children.add(new NodeLink(childLink,rootLink));
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing child node link: " + link);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing children links: " + e.getMessage());
        }
        D_S.markEndAndGet();
    }
    public Set<NodeLink> getChildren()
    {
        initChildren();
        return children;
    }

}