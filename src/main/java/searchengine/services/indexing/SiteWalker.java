package searchengine.services.indexing;
import lombok.Getter;
import lombok.extern.log4j.Log4j;
import org.jsoup.Connection;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RecursiveAction;
@Log4j
@Getter
public class SiteWalker extends RecursiveAction {
    //field and constructor:
    public SiteWalker(NodeLink curNodeLink, Site rootSite, PageRepository pageRepository, SiteRepository siteRepository) {
        this.curNodeLink = curNodeLink;
        this.rootSite = rootSite;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }
    private final NodeLink curNodeLink;
    private final Site rootSite;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private static final Set<String> VISITED_LINKS = new HashSet<>();
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
                    String pathLink = getPathOf(absoluteLink);
                    if (notVisited(absoluteLink) && !pathLink.isEmpty() && rootSite.isIndexing())
                    {
                        System.out.println(rootSite.isIndexing());
                        new SiteWalker(child, rootSite,pageRepository,siteRepository).fork();
                        Connection.Response response = curNodeLink.getResponse();
                        Page curPage = new Page(
                                rootSite,
                                pathLink,
                                response.statusCode(),
                                response.parse().toString()
                        );
                        pageRepository.save(curPage);
                        setCurrentTimeToRootSite();
                        System.out.println("\u001B[32m" + "Добавлена новая страница с путём: " + pathLink + "\u001B[0m" + " от сайта " + rootSite.getId());
                    }

                } catch (Exception e)
                {
                    System.out.println("ОШИБКА ОТ GETTASKS(): + trace:");
                    e.printStackTrace();
                }
            });
    }
    public String getPathOf(String link) throws URISyntaxException {return new URI(link).getPath();}
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
        siteRepository.save(rootSite);
    }
}