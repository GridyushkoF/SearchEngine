package searchengine.services.indexing;
import lombok.Getter;
import lombok.extern.log4j.Log4j;
import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;
import searchengine.services.RepoService;
import searchengine.services.lemmas.LemmatizationService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Log4j
@Getter
public class SiteWalker extends RecursiveAction {
    //data:
    public SiteWalker(NodeLink curNodeLink, Site rootSite, RepoService repoService) {
        this.curNodeLink = curNodeLink;
        this.rootSite = rootSite;
        this.repoService = repoService;
        this.lemmatizationService = new LemmatizationService(repoService);
        this.pageRepo = repoService.getPageRepo();

    }
    private final NodeLink curNodeLink;
    private final Site rootSite;
    private final RepoService repoService;
    private static final Set<String> VISITED_LINKS = Collections.synchronizedSet(new HashSet<>());
    private final LemmatizationService lemmatizationService;
    private final Set<Page> tmpPages = new HashSet<>();
    private final PageRepository pageRepo;
    //alg.code:
    @Override

    protected void compute() {
        getTasks();
    }
    @Transactional
    public void getTasks() {
        try {
            curNodeLink.getChildren().forEach(child -> {
                String absoluteLink = child.getLink();
                String pathLink = IndexUtils.getPathOf(absoluteLink);
                if (notVisited(absoluteLink) && !pathLink.isEmpty() && IndexingService.isIndexing()) {
                    new SiteWalker(child, rootSite, repoService).fork();
                    Connection.Response response = IndexUtils.getResponse(absoluteLink);
                    try {
                    assert response != null;
                    tmpPages.add(new Page(
                            rootSite,
                            pathLink,
                            response.statusCode(),
                            response.parse().toString()));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    setCurrentTimeToRootSite();
                    System.out.println("\u001B[32m" + "Новая страница в пачке!: " + pathLink + "\u001B[0m" + " от сайта " + rootSite.getId());
                }
            });
        }catch (Exception e) {
            System.out.println("Ошибка от SiteWalker");
        }
        pageRepo.saveAll(tmpPages);
        tmpPages.forEach(lemmatizationService::addToIndex);
        tmpPages.clear();
    }


    private boolean notVisited(String link)
    {
        if (!VISITED_LINKS.contains(link))
        {
            VISITED_LINKS.add(link);
            return true;
        }
        return false;
    }
    private void setCurrentTimeToRootSite ()
    {
        rootSite.setStatusTime(LocalDateTime.now());
        repoService.getSiteRepo().save(rootSite);
    }
}