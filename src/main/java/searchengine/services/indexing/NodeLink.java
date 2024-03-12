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

    private void initChildrenIfLinkIsValid() {
        try {
            if(!IndexingService.isIndexing()){
                return;
            }
            if(!IndexingUtils.isAppropriateLink(link)) {
                log.error("Exception: " + "ссылка: " + link + " не подходящая");
                return;
            }
            startInitialization();
        } catch (Exception e) {
            log.error(LogMarkers.EXCEPTIONS,"Exception while children init: " + link, e);
        }
    }

    private void startInitialization() throws Exception {
        Document htmlDocument = Jsoup.connect(link).get();
        for (Element element : htmlDocument.select("a")) {
            String link = element.attr("href");
            link = normalizeLink(link);
            addChildIfValidByLink(link);
        }
    }

    private void addChildIfValidByLink(String link) throws Exception {
        if (IndexingUtils.isAppropriateLink(link)) {
            String childLink = (link.startsWith("/") ? (rootLink + link) : link);
            if (IndexingUtils.compareHosts(childLink, this.link)
                    &&
                children.stream().noneMatch(child -> child.getLink().equals(childLink))) {
                children.add(new NodeLink(childLink, rootLink));
            }
        }
    }

    private String normalizeLink (String link) {
        link = link.replaceAll(" ", "");
        link = link.endsWith("/") ? link : link + "/";
        return link;
    }

    public Set<NodeLink> getChildren() {
        if(children.isEmpty()) {
            initChildrenIfLinkIsValid();
        }
        return children;
    }
}