package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
                if (IndexUtils.isNormalLink(href)) {
                    NodeLink childNodeLink = new NodeLink((href.startsWith("/") ? (rootLink + href) : href), rootLink);
                    String childLink = childNodeLink.getLink();
                    try {
                        if (IndexUtils.compareHosts(childLink,link)
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
    public Set<NodeLink> getChildren()
    {
        initChildren();
        return children;
    }

}