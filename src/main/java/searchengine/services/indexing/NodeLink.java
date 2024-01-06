package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.services.other.IndexingUtils;
import searchengine.services.other.LogService;

import java.util.HashSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
@ToString(of = {"link"})
public class NodeLink {
    private final String link;
    private final String rootLink;
    private final Set<NodeLink> children = new HashSet<>();
    private static final LogService LOGGER = new LogService();

    private void initChildren() {
        try {
            Document doc = Jsoup.connect(link).get();
            for (Element element : doc.select("a")) {
                String link = element.attr("href");
                link = link
                        .replaceAll(" ", "")
                        .replaceAll("//", "/")
                        .replaceAll("https:/", "https://")
                        .replaceAll("http:/", "http://");
                if (IndexingUtils.isAppropriateLink(link)) {
                    String childLink = (link.startsWith("/") ? (rootLink + link) : link);
                    try {
                        if (IndexingUtils.compareHosts(childLink, this.link)
                                &&
                                children.stream().noneMatch(child -> child.getLink().equals(childLink))) {
                            children.add(new NodeLink(childLink, rootLink));
                        }
                    } catch (Exception e) {
                        LOGGER.exception(e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.exception(e);
        }
    }

    public Set<NodeLink> getChildren() {
        initChildren();
        return children;
    }
}