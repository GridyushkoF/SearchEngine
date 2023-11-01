package searchengine.services.indexing;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.jsoup.Connection;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.model.*;
import searchengine.services.RepoService;
import searchengine.services.lemmas.LemmatizationService;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Component
@Log4j
public class IndexingService {
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    private final LemmaRepository lemmaRepo;
    private final SearchIndexRepository indexRepository;
    private final RepoService repoService;
    private static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);
    public IndexingService(RepoService repoService) {
        this.repoService = repoService;
        this.siteRepo = repoService.getSiteRepo();
        this.pageRepo = repoService.getPageRepo();
        this.lemmaRepo = repoService.getLemmaRepo();
        this.indexRepository = repoService.getIndexRepo();

    }
    private ExecutorService executorService;
    private static final ForkJoinPool pool = new ForkJoinPool();
    public void startIndexing() {
        if (IS_INDEXING.compareAndSet(false, true)) {
            List<ConfigSite> configSiteList = YamlParser.getSitesFromYaml();
            pageRepo.deleteAll();
            siteRepo.deleteAll();
            indexRepository.deleteAll();
            lemmaRepo.deleteAll();
            executorService = Executors.newFixedThreadPool(configSiteList.size());
            configSiteList.forEach(configSite -> {
                executorService.submit(() -> {
                    Site site = new Site(
                            "INDEXING",
                            LocalDateTime.now(),
                            null,
                            configSite.getUrl(),
                            configSite.getName());
                    siteRepo.save(site);
                    //init data:
                    NodeLink currentSiteNodeLink = new NodeLink(
                            configSite.getUrl(),
                            configSite.getUrl()
                    );
                    SiteWalker walker = null;
                    try {
                        walker = new SiteWalker(currentSiteNodeLink, site, new RepoService(lemmaRepo,pageRepo,indexRepository,siteRepo));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    addListenerToDone(walker);
                    System.out.println("УСПЕШНО ДОБАВЛЕН В ИНДЕКСАЦИЮ САЙТ: " + site.getUrl());
                });
            });
        }
    }

    @SneakyThrows
    @Async
    public void addListenerToDone(SiteWalker walker){
            pool.invoke(walker); // Запускаем выполнение задачи SiteWalker
            // Ожидаем завершения всех подзадач
            while (!walker.isDone()) {
                Site curWalkerRootSite = walker.getRootSite();
                curWalkerRootSite.setStatus("INDEXED");
                curWalkerRootSite.setStatusTime(LocalDateTime.now());
                System.out.println(curWalkerRootSite.getName() + " успешно окончил индексирование!");
                IS_INDEXING.set(false);
                executorService.shutdownNow();
            }

    }

    @Transactional
    public void stopAllSites(){
        executorService.shutdownNow();
       IS_INDEXING.set(false);
        siteRepo.findAll().forEach(site -> {
            site.setStatus("FAILED");
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Индексация остановлена пользователем!");
            siteRepo.save(site);
            try {
                Thread.sleep(1000);
            }catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("!!!!!!!!!!!! " + siteRepo.findByUrl(site.getUrl()));

        });
    }

    public static boolean isIndexing() {
        return IS_INDEXING.get();
    }
    public boolean indexPage(String url) {
        boolean isPageInSites = false;
        String rootSiteUrl = "";
        try {
            for (ConfigSite site : YamlParser.getSitesFromYaml()) {
                if (IndexUtils.compareHosts(url,site.getUrl())) {
                    isPageInSites = true;
                    rootSiteUrl = site.getUrl();
                    break;
                }
            }
            if (isPageInSites) {
                Optional<Page> pageOPT = pageRepo.findByPath(IndexUtils.getPathOf(url));
                LemmatizationService service = new LemmatizationService(repoService);
                if (pageOPT.isPresent()) {
                    Page page = pageOPT.get();
                    indexRepository.findAllByPage(page).forEach(index -> {

                        lemmaRepo.findByLemma(index.getLemma().getLemma())
                                .ifPresent(lemmaRepo::delete);
                        indexRepository.delete(index);
                    });
                    pageRepo.delete(page);
                }
                if (repoService.getSiteRepo().findByUrl(rootSiteUrl).isPresent()) {
                    Connection.Response response = IndexUtils.getResponse(url);
                    service.addToIndex(
                            new Page(
                                    repoService.getSiteRepo().findByUrl(rootSiteUrl).get(),
                                    IndexUtils.getPathOf(url),
                                    response.statusCode(),
                                    response.parse().toString()
                            )
                    );
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}