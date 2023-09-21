package searchengine.services.indexing;
import org.jsoup.Connection;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
public class SiteWalker extends RecursiveAction {
    //field and constructor:
    public SiteWalker(NodeLink curNodeLink, Site rootSite, PageRepository pageRepository, SiteRepository siteRepository, Set<String> visited_links) {
        this.curNodeLink = curNodeLink;
        this.rootSite = rootSite;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.visited_links = visited_links;
        stringLink = curNodeLink.getLink();
    }
    private final NodeLink curNodeLink;
    private final Site rootSite;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final String stringLink;
    private final Set<String> visited_links;
    //alg.code:
    @Override
    protected void compute() {
        try {
            getTasks().forEach(ForkJoinTask::join);
        }catch (Exception e) {e.printStackTrace();}
    }
    private List<SiteWalker> getTasks() throws URISyntaxException {
        markAsVisited(convertNodeLinkToPath());
        List<SiteWalker> taskList = new ArrayList<>();
        curNodeLink.getChildren().forEach(child -> {
            try {
                if (notVisited(child.getLink()))
                {
                    SiteWalker task = new SiteWalker(child, rootSite,pageRepository,siteRepository, visited_links);
                    task.fork();
                    markAsVisited(child.getLink());
                    taskList.add(task);
                    Connection.Response response = curNodeLink.getResponse();
                    Page curPage = new Page(
                            rootSite,
                            !convertNodeLinkToPath().isEmpty() ? convertNodeLinkToPath() : "/",
                            response.statusCode(),
                            response.parse().toString()
                    );
                    pageRepository.save(curPage);
                    setCurrentTimeToRootSite();
                    System.out.println("\u001B[32m" + "Добавлена новая страница с путём: " + (stringLink.equals(rootSite.getUrl()) ? "/" : convertNodeLinkToPath()) + "\u001B[0m");
                }
            } catch (Exception e)
            {
                System.out.println("ОШИБКА ОТ GETTASKS(): + trace:");
                e.printStackTrace();
            }
        });
        return taskList;
    }
    public String convertNodeLinkToPath() throws URISyntaxException {return new URI(stringLink).getPath();}
    private boolean notVisited(String link) {return !visited_links.contains(link);}
    private void markAsVisited (String link) {visited_links.add(link);}
    private void setCurrentTimeToRootSite ()
    {
        rootSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(rootSite);
    }
}