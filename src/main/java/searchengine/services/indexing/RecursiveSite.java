package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.PageStatus;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.util.IndexingUtil;
import searchengine.util.LogMarkersUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

@Getter
@Log4j2
@RequiredArgsConstructor
public class RecursiveSite extends RecursiveAction {
    private static final Set<String> VISITED_LINKS = Collections.synchronizedSet(new HashSet<>());
    private final Set<Page> tempPages = new HashSet<>();
    private final NodeLink currentNodeLink;
    private final Site rootSite;
    private final RecursiveSiteDbProxy dbProxy;


    public static void clearVisitedLinks() {
        VISITED_LINKS.clear();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void compute() {
        currentNodeLink.initChildrenIfEmptyOrGet().forEach(childNodeLink -> {
            String absoluteLink = childNodeLink.getLink();
            String pathLink = IndexingUtil.getPathOf(absoluteLink);
            if (isNotVisited(absoluteLink) && !pathLink.isEmpty() && (rootSite.getStatus().equals(SiteStatus.INDEXING))) {
                initAndForkRecursiveSite(childNodeLink);
                initAndAddPageToTempPages(childNodeLink, absoluteLink, pathLink);
                if (IndexingService.isIndexing()) {
                    dbProxy.setCurrentTimeToRootSite(rootSite);
                }
            }
        });
        if(currentNodeLink.initChildrenIfEmptyOrGet().isEmpty()) {
            System.out.println("Ссылка с пустыми детьми: " + currentNodeLink.getLink());
        }
        if (rootSite.getStatus().equals(SiteStatus.INDEXING) && IndexingService.isIndexing()) {
            dbProxy.saveAndClearTempPages(currentNodeLink,tempPages);
        }
    }
    private void initAndAddPageToTempPages(NodeLink nodeLink, String absoluteLink, String pathLink) {
        Connection.Response response = IndexingUtil.getResponse(absoluteLink);
        try {
            if(response != null) {
                tempPages.add(new Page(
                        rootSite,
                        pathLink,
                        response.statusCode(),
                        response.parse().toString(), PageStatus.INIT));
            }
        } catch (IOException e) {
            log.error(LogMarkersUtil.EXCEPTIONS, "Can`t save child: " + nodeLink.getLink(), e);
        }
    }

    private void initAndForkRecursiveSite(NodeLink nodeLink) {
        new RecursiveSite(nodeLink,
                rootSite,
                dbProxy).fork();
    }

    private boolean isNotVisited(String link) {
        if (!VISITED_LINKS.contains(link)) {
            VISITED_LINKS.add(link);
            return true;
        }
        return false;
    }

}