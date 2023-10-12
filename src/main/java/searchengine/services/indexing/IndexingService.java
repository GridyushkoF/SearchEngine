package searchengine.services.indexing;

import lombok.extern.log4j.Log4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@Component
@Log4j
public class IndexingService {
    private SiteRepository siteRepo;
    private PageRepository pageRepo;

    public IndexingService(SiteRepository siteRepo, PageRepository pageRepo) {
        this.siteRepo = siteRepo;
        this.pageRepo = pageRepo;
    }
    private ExecutorService executorService;
    private static final ForkJoinPool pool = new ForkJoinPool();
    public void startIndexing () {
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
                SiteWalker walker = new SiteWalker(currentSiteNodeLink, site, pageRepo, siteRepo);
                addListenerToDone(walker);
                System.out.println("УСПЕШНО ДОБАВЛЕН В ИНДЕКСАЦИЮ САЙТ: " + site.getUrl());
            });

        });
    }
    @Async
    public void addListenerToDone(SiteWalker walker) {

        pool.invoke(walker); // Запускаем выполнение задачи SiteWalker
        // Ожидаем завершени всех подзадач
        walker.join();
        Site curWalkerRootSite = walker.getRootSite();
        curWalkerRootSite.setStatus("INDEXED");
        curWalkerRootSite.setStatusTime(LocalDateTime.now());
        System.out.println(curWalkerRootSite.getName() + " успешно окончил индексирование!");
    }
    @Transactional
    public void stopSite(Site site) {
        try {
            executorService.shutdownNow();
            executorService.awaitTermination(100,TimeUnit.MILLISECONDS);
            pool.awaitTermination(1, TimeUnit.MILLISECONDS);
            pool.shutdownNow();

        }catch (Exception e) {
            e.printStackTrace();
        }
        site.setStatus("FAILED");
        site.setStatusTime(LocalDateTime.now());
        site.setLastError("Индексация остановлена пользователем!");
        siteRepo.save(site);


        System.out.println("!!!!!!!!!!!! " + siteRepo.findByUrl(site.getUrl()));

    }
}