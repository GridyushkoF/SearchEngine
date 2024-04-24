package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.util.IndexingUtil;
import searchengine.util.LogMarkersUtil;

import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
@ToString(of = {"link"})
@Log4j2
public class NodeLink {
    private String link;
    private final String rootLink;
    private final Set<NodeLink> children = new HashSet<>();

    public Set<NodeLink> initChildrenIfEmptyOrGet() {
        if(children.isEmpty()) {
            initChildrenIfLinkIsValid();
        }
        return children;
    }

    private void initChildrenIfLinkIsValid() {
        if(!IndexingService.isIndexing()){
            return;
        }
        link = normalizeLink(link);
        if(!IndexingUtil.isAppropriateLink(link)) {
            log.error("Exception: " + "ссылка: " + link + " не подходящая");
            return;
        }
        initChildrenWithTryCatch();
    }

    private void initChildrenWithTryCatch() {
        try {
            initChildren();
        } catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS,"Не удалось проинициализировать страницу: " + link);
            log.error(e);
            if(e.getMessage().contains("timed out")) {
                log.error(LogMarkersUtil.EXCEPTIONS,"Повторяем попытку инициализации, подождите...");
                initChildrenWithTryCatch();
            }
        }
    }
    private void initChildren() throws Exception {
        Document htmlDocument = Jsoup.connect(link).get();
        for (Element element : htmlDocument.select("a")) {
            String hrefLink = element.attr("href");
            hrefLink = normalizeLink(hrefLink);
            addChildIfLinkIsValid(hrefLink);
        }
    }

    private void addChildIfLinkIsValid(String link) throws Exception {
        if (IndexingUtil.isAppropriateLink(link)) {
            String childLink = (link.startsWith("/") ? (rootLink + link) : link);
            if (IndexingUtil.compareHosts(childLink, this.link)
                    &&
                children.stream().noneMatch(child -> child.getLink().equals(childLink))) {
                children.add(new NodeLink(childLink, rootLink));
            }
        }
    }

    private String normalizeLink (String link) {
        link = link.trim();
        link = link.endsWith("/") ? link : link + "/";
        link = link.replaceAll(" ","%20");
        return link;
    }
}