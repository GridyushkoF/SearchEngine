package searchengine.services.indexing;

import lombok.extern.log4j.Log4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.model.*;
import searchengine.services.RepoService;
import searchengine.services.lemmas.LemmatizationService;
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
    private final SearchIndexRepository indexRepository;
    private final RepoService repoService;
    private static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);
    private final LemmatizationService lemmatizationService;

    private ExecutorService executorService;
    private static final ForkJoinPool pool = new ForkJoinPool();
    public IndexingService(RepoService repoService) {
        this.repoService = repoService;
        this.siteRepo = repoService.getSiteRepo();
        this.pageRepo = repoService.getPageRepo();
        this.lemmaRepo = repoService.getLemmaRepo();
        this.indexRepository = repoService.getIndexRepo();
        this.lemmatizationService = new LemmatizationService(repoService);

    }
    public void startIndexing() {
        if (!IS_INDEXING.get()) {
            IS_INDEXING.set(true);
            List<ConfigSite> configSiteList = YamlParser.getSitesFromYaml();
            indexRepository.deleteAll();
            lemmaRepo.deleteAll();
            pageRepo.deleteAll();
            siteRepo.deleteAll();

            executorService = Executors.newFixedThreadPool(configSiteList.size());
            configSiteList.forEach(configSite -> {
                executorService.submit(() -> {
                    var site = new Site(
                            "INDEXING",
                            LocalDateTime.now(),
                            null,
                            configSite.getUrl(),
                            configSite.getName());
                    siteRepo.save(site);
                    //init data:
                    var currentSiteNodeLink = new NodeLink(
                            configSite.getUrl(),
                            configSite.getUrl()
                    );
                    var walker = new SiteWalker(currentSiteNodeLink, site, new RepoService(lemmaRepo,pageRepo,indexRepository,siteRepo));
                    addListenerToDone(walker);
                    System.out.println("УСПЕШНО ДОБАВЛЕН В ИНДЕКСАЦИЮ САЙТ: " + site.getUrl());
                });
            });
        }
    }

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
            pool.shutdownNow();
            System.out.println("!!!!!!!!!!!! " + siteRepo.findByUrl(site.getUrl()));

        });
    }

    public static boolean isIndexing() {
        return IS_INDEXING.get();
    }
    @Transactional
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
                var pageOPT = pageRepo.findByPath(IndexUtils.getPathOf(url));
                var service = new LemmatizationService(repoService);
                if (pageOPT.isPresent()) {
                    var page = pageOPT.get();
                    var indexes = indexRepository.findAllByPage(page);
                    indexes.forEach(index -> {
                        var lemmaOpt = lemmaRepo.findByLemma(index.getLemma().getLemma());
                        lemmaOpt.ifPresent(lemma -> {
                            lemma.setFrequency(lemma.getFrequency() - 1);
                        });
                        indexRepository.delete(index);
                    });
                    pageRepo.delete(page);
                }
                if (repoService.getSiteRepo().findByUrl(rootSiteUrl).isPresent()) {
                    var response = IndexUtils.getResponse(url);
                    Page page = new Page(
                            repoService.getSiteRepo().findByUrl(rootSiteUrl).get(),
                            IndexUtils.getPathOf(url),
                            response.statusCode(),
                            response.parse().toString()
                    );
                    pageRepo.save(page);
                    service.addToIndex(page);
                    System.out.printf("СТРАНИЦА %s УСПЕШНО ИНДЕКСИРОВАНА\n",page.getPath());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}