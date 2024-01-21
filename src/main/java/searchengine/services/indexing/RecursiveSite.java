package searchengine.services.indexing;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.LemmatizationService;
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
public class RecursiveSite extends RecursiveAction {
    private static final Set<String> VISITED_LINKS = Collections.synchronizedSet(new HashSet<>());
    private final NodeLink currentNodeLink;
    private final Site rootSite;
    private final LemmatizationService lemmatizationService;
    private final Set<Page> tmpPages;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final SearchIndexRepository indexRepo;

    public RecursiveSite(NodeLink currentNodeLink,
                         Site rootSite,
                         LemmatizationService lemmatizationService,
                         PageRepository pageRepository,
                         LemmaRepository lemmaRepository,
                         SiteRepository siteRepository,
                         SearchIndexRepository indexRepo) {
        this.currentNodeLink = currentNodeLink;
        this.rootSite = rootSite;
        this.lemmatizationService = lemmatizationService;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
        this.indexRepo = indexRepo;
        tmpPages = new HashSet<>();
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
            currentNodeLink.getChildren().forEach(child -> {
                String absoluteLink = child.getLink();
                String pathLink = IndexingUtils.getPathOf(absoluteLink);
                if (notVisited(absoluteLink) && !pathLink.isEmpty() && IndexingService.isIndexing()) {
                    new RecursiveSite(child,
                            rootSite,
                            lemmatizationService,
                            pageRepository,
                            lemmaRepository,
                            siteRepository,
                            indexRepo).fork();
                    Connection.Response response = IndexingUtils.getResponse(absoluteLink);
                    try {
                        assert response != null;
                        tmpPages.add(new Page(
                                rootSite,
                                pathLink,
                                response.statusCode(),
                                response.parse().toString()));
                    } catch (IOException e) {
                        log.error(LogMarkers.EXCEPTIONS,"Can`t save child: " + child.getLink(), e);
                    }
                    setCurrentTimeToRootSite();
                }
            });

        pageRepository.saveAll(tmpPages);
        log.info(LogMarkers.INFO,"All pages saved for this link: " + currentNodeLink.getLink());
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
        siteRepository.save(rootSite);
    }
}