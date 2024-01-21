package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.util.IndexingUtils;
import searchengine.util.LogMarkers;

import java.util.HashSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
@ToString(of = {"link"})
@Log4j2
public class NodeLink {
    private final String link;
    private final String rootLink;
    private final Set<NodeLink> children = new HashSet<>();

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
                    if (IndexingUtils.compareHosts(childLink, this.link)
                            &&
                            children.stream().noneMatch(child -> child.getLink().equals(childLink))) {
                        children.add(new NodeLink(childLink, rootLink));
                    }
                }
            }
        } catch (Exception e) {
            log.error(LogMarkers.EXCEPTIONS,"Exception while children init: " + link, e);
        }
    }

    public Set<NodeLink> getChildren() {
        initChildren();
        return children;
    }
}