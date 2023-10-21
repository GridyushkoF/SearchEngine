package searchengine.services.indexing;

import lombok.extern.log4j.Log4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.model.LemmaRepository;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
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
    private static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);

    public IndexingService(SiteRepository siteRepo, PageRepository pageRepo, LemmaRepository lemmaRepo) {
        this.siteRepo = siteRepo;
        this.pageRepo = pageRepo;
        this.lemmaRepo = lemmaRepo;
    }

    private ExecutorService executorService;
    private static final ForkJoinPool pool = new ForkJoinPool();

    public void startIndexing() {
        if (IS_INDEXING.compareAndSet(false, true)) {
            List<ConfigSite> configSiteList = YamlParser.getSitesFromYaml();
            pageRepo.deleteAll();
            siteRepo.deleteAll();

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
                    SiteWalker walker = new SiteWalker(currentSiteNodeLink, site, pageRepo, siteRepo,lemmaRepo);
                    addListenerToDone(walker);
                    System.out.println("УСПЕШНО ДОБАВЛЕН В ИНДЕКСАЦИЮ САЙТ: " + site.getUrl());
                });
            });
        }
    }

    @Async
    public void addListenerToDone(SiteWalker walker) {
        pool.invoke(walker); // Запускаем выполнение задачи SiteWalker
        // Ожидаем завершения всех подзадач
        walker.join();
        Site curWalkerRootSite = walker.getRootSite();
        curWalkerRootSite.setStatus("INDEXED");
        curWalkerRootSite.setStatusTime(LocalDateTime.now());
        System.out.println(curWalkerRootSite.getName() + " успешно окончил индексирование!");
        IS_INDEXING.set(false);
        executorService.shutdownNow();
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
}