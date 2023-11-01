package searchengine.services.indexing;
import lombok.Getter;
import lombok.extern.log4j.Log4j;
import org.jsoup.Connection;
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
    //field and constructor:
    public SiteWalker(NodeLink curNodeLink, Site rootSite, RepoService repoService) throws IOException {
        this.curNodeLink = curNodeLink;
        this.rootSite = rootSite;
        this.repoService = repoService;

        this.lemmatizationService = new LemmatizationService(repoService);

    }
    private final NodeLink curNodeLink;
    private final Site rootSite;
    private final RepoService repoService;


    private static final Set<String> VISITED_LINKS = new HashSet<>();
    private final LemmatizationService lemmatizationService;


    private static final Set<String> VISITED_LEMMAS = new HashSet<>();



    //alg.code:
    @Override
    protected void compute() {
        getTasks();
    }
    @Transactional
    public void getTasks() {
        curNodeLink.getChildren().forEach(child -> {
            try {
                String absoluteLink = child.getLink();
                String pathLink = IndexUtils.getPathOf(absoluteLink);
                if (notVisited(absoluteLink) && !pathLink.isEmpty() && IndexingService.isIndexing()) {
                    System.out.println("Состояние сервиса индексации: " + IndexingService.isIndexing());
                    new SiteWalker(child, rootSite, repoService).fork();
                    Connection.Response response = IndexUtils.getResponse(absoluteLink);
                    Page curPage = new Page(
                            rootSite,
                            pathLink,
                            response.statusCode(),
                            response.parse().toString()
                    );
                    repoService.getPageRepo().save(curPage);
                    setCurrentTimeToRootSite();
                    System.out.println("\u001B[32m" + "Добавлена новая страница с путём: " + pathLink + "\u001B[0m" + " от сайта " + rootSite.getId());
                    lemmatizationService.addToIndex(curPage);
                }

            } catch (Exception e) {
                System.out.println("ОШИБКА ОТ GETTASKS(): + trace:");
//                        e.printStackTrace();
            }
        });
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