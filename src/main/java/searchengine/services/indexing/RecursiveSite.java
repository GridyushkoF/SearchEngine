package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.PageStatus;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.IndexingUtils;
import searchengine.util.LogMarkers;

import java.io.IOException;
import java.time.LocalDateTime;
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
    private final LemmaService lemmaService;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final SearchIndexRepository indexRepository;

    public static void clearVisitedLinks() {
        VISITED_LINKS.clear();
    }

    @Override
    @Transactional
    public void compute() {
        getTasks();
    }

    @Transactional
    public void getTasks() {
        currentNodeLink.getChildren().forEach(childNodeLink -> {
            String absoluteLink = childNodeLink.getLink();
            String pathLink = IndexingUtils.getPathOf(absoluteLink);
            if (isNotVisited(absoluteLink) && !pathLink.isEmpty() && (rootSite.getStatus().equals(SiteStatus.INDEXING))) {
                initAndForkRecursiveSite(childNodeLink);
                initAndAddPageToTempPages(childNodeLink, absoluteLink, pathLink);
                if (IndexingService.isIndexing()) {
                    setCurrentTimeToRootSite();
                }
            }
        });
        if (rootSite.getStatus().equals(SiteStatus.INDEXING) && IndexingService.isIndexing()) {
            saveAndClearTempPages();
        }
    }

    private void initAndAddPageToTempPages(NodeLink nodeLink, String absoluteLink, String pathLink) {
        Connection.Response response = IndexingUtils.getResponse(absoluteLink);
        try {
            assert response != null;
            tempPages.add(new Page(
                    rootSite,
                    pathLink,
                    response.statusCode(),
                    response.parse().toString(), PageStatus.INIT));
        } catch (IOException e) {
            log.error(LogMarkers.EXCEPTIONS, "Can`t save child: " + nodeLink.getLink(), e);
        }
    }

    private void initAndForkRecursiveSite(NodeLink nodeLink) {
        new RecursiveSite(nodeLink,
                rootSite,
                lemmaService,
                pageRepository,
                lemmaRepository,
                siteRepository,
                indexRepository).fork();
    }

    private void saveAndClearTempPages() {
        pageRepository.saveAll(tempPages);
        log.info(LogMarkers.INFO, "PAGE INIT: " + currentNodeLink.getLink());
        tempPages.forEach(page -> lemmaService.getAndSaveLemmasAndIndexes(page, false));
        tempPages.clear();
    }

    private boolean isNotVisited(String link) {
        if (!VISITED_LINKS.contains(link)) {
            VISITED_LINKS.add(link);
            return true;
        }
        return false;
    }

    @Transactional
    public void setCurrentTimeToRootSite() {
        rootSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(rootSite);
    }
}