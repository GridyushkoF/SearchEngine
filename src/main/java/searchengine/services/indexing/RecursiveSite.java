package searchengine.services.indexing;

import lombok.Getter;
import org.jsoup.Connection;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepo;
import searchengine.services.lemmas.LemmatizationService;
import searchengine.services.other.IndexingUtils;
import searchengine.services.other.LogService;
import searchengine.services.other.RepoService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

@Getter
public class RecursiveSite extends RecursiveAction {
    private static final Set<String> VISITED_LINKS = Collections.synchronizedSet(new HashSet<>());
    private static final LogService LOGGER = new LogService();
    private final NodeLink currentNodeLink;
    private final Site rootSite;
    private final RepoService repoService;
    private final LemmatizationService lemmatizationService;
    private final Set<Page> tmpPages = new HashSet<>();
    private final PageRepo pageRepo;

    public RecursiveSite(NodeLink currentNodeLink, Site rootSite, RepoService repoService) {
        this.currentNodeLink = currentNodeLink;
        this.rootSite = rootSite;
        this.repoService = repoService;
        this.lemmatizationService = new LemmatizationService(repoService);
        this.pageRepo = repoService.getPageRepo();
    }

    public static void clearVisitedLinks() {
        VISITED_LINKS.clear();
    }

    @Override
    protected void compute() {
        getTasks();
    }

    @Transactional
    public void getTasks() {
        try {
            currentNodeLink.getChildren().forEach(child -> {
                String absoluteLink = child.getLink();
                String pathLink = IndexingUtils.getPathOf(absoluteLink);
                if (notVisited(absoluteLink) && !pathLink.isEmpty() && IndexingService.isIndexing()) {
                    new RecursiveSite(child, rootSite, repoService).fork();
                    Connection.Response response = IndexingUtils.getResponse(absoluteLink);
                    try {
                        assert response != null;
                        tmpPages.add(new Page(
                                rootSite,
                                pathLink,
                                response.statusCode(),
                                response.parse().toString()));
                    } catch (IOException e) {
                        LOGGER.exception(e);
                    }
                    setCurrentTimeToRootSite();
                }
            });
        } catch (Exception e) {
            LOGGER.exception(e);
        }
        pageRepo.saveAll(tmpPages);
        LOGGER.info(currentNodeLink.getLink() + " all pages saved for this link");
        tmpPages.forEach(lemmatizationService::getAndSaveLemmasAndIndexes);
        tmpPages.clear();
    }

    private boolean notVisited(String link) {
        if (!VISITED_LINKS.contains(link)) {
            VISITED_LINKS.add(link);
            return true;
        }
        return false;
    }

    private void setCurrentTimeToRootSite() {
        rootSite.setStatusTime(LocalDateTime.now());
        repoService.getSiteRepo().save(rootSite);
    }
}