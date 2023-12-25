package searchengine.services.indexing;

import lombok.extern.log4j.Log4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.model.*;
import searchengine.repositories.LemmaRepo;
import searchengine.repositories.PageRepo;
import searchengine.repositories.SearchIndexRepo;
import searchengine.repositories.SiteRepo;
import searchengine.services.other.MyFjpThreadFactory;
import searchengine.services.other.RepoService;
import searchengine.services.lemmas.LemmatizationService;
import searchengine.services.other.IndexingUtils;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Component
@Log4j
public class IndexingService {
    private final SiteRepo siteRepo;
    private final PageRepo pageRepo;
    private final LemmaRepo lemmaRepo;
    private final SearchIndexRepo indexRepo;
    private final RepoService repoService;
    private static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);
    private final List<ConfigSite> configSiteList;
    private final List<ForkJoinPool> forkJoinPoolList = new ArrayList<>();
    private ExecutorService executorService;
    public IndexingService(RepoService repoService) {
        this.repoService = repoService;
        this.siteRepo = repoService.getSiteRepo();
        this.pageRepo = repoService.getPageRepo();
        this.lemmaRepo = repoService.getLemmaRepo();
        this.indexRepo = repoService.getIndexRepo();
        this.configSiteList = YamlParser.getSitesFromYaml();
    }
    public void startIndexing() {
        if (!IS_INDEXING.get()) {
            RecursiveSite.clearVisitedLinks();
            IS_INDEXING.set(true);
            indexRepo.deleteAll();
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
                    var currentSiteNodeLink = new NodeLink(
                            configSite.getUrl(),
                            configSite.getUrl()
                    );
                    var walker = new RecursiveSite(currentSiteNodeLink, site, new RepoService(lemmaRepo,pageRepo, indexRepo,siteRepo));
                    addStopIndexingListener(walker);
                    System.out.println("УСПЕШНО ДОБАВЛЕН В ИНДЕКСАЦИЮ САЙТ: " + site.getUrl());
                });
            });
        }
    }
    @Async
    public void addStopIndexingListener(RecursiveSite walker){
        ForkJoinPool forkJoinPool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors(),
                new MyFjpThreadFactory(),
                (t, e) -> {
                    System.err.println(t.getName() + " " + e.getMessage());
                },
                true
        );
        forkJoinPool.invoke(walker);
        forkJoinPoolList.add(forkJoinPool);
        while (forkJoinPool.getActiveThreadCount() > 0) {
            //wait
        }
        deleteAllDuplicates();
        IS_INDEXING.set(false);
        Site curWalkerRootSite = walker.getRootSite();
        curWalkerRootSite.setStatus("INDEXED");
        curWalkerRootSite.setStatusTime(LocalDateTime.now());
        System.out.println(curWalkerRootSite.getName() + " успешно окончил индексирование!");
        executorService.shutdownNow();

    }
    @Transactional
    public void stopAllSitesByUser(){
        deleteAllDuplicates();
        IS_INDEXING.set(false);
        executorService.shutdownNow();
        siteRepo.findAll().forEach(DBSite -> {
            DBSite.setStatus("FAILED");
            DBSite.setStatusTime(LocalDateTime.now());
            DBSite.setLastError("Индексация остановлена пользователем!");
            siteRepo.save(DBSite);
            try {
                Thread.sleep(1000);
            }catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("!!! Сайт остановлен пользователем: " + siteRepo.findByUrl(DBSite.getUrl()));
        });
        forkJoinPoolList.forEach(ForkJoinPool::shutdown);
    }

    @Transactional
    public boolean reindexPage(String url) {
        boolean isPageInSitesRange = false;
        String rootSiteUrl = "";
        try {
            for (var configSite : configSiteList) {
                if (IndexingUtils.compareHosts(url,configSite.getUrl())) {
                    isPageInSitesRange = true;
                    rootSiteUrl = configSite.getUrl();
                    break;
                }
            }
            if (isPageInSitesRange) {
                var pageOPT = pageRepo.findByPath(IndexingUtils.getPathOf(url));
                var lemmatizationService = new LemmatizationService(repoService);
                if (pageOPT.isPresent()) {
                    var page = pageOPT.get();
                    var indexes = indexRepo.findAllByPage(page);
                    indexes.forEach(DBIndex -> {
                        var lemmaList = lemmaRepo.findAllByLemma(DBIndex.getLemma().getLemma());
                        if(lemmaList.size() > 0) {
                            var lemma = lemmaList.get(0);
                            lemma.setFrequency(lemma.getFrequency() - 1);
                        }
                        indexRepo.delete(DBIndex);
                    });
                    pageRepo.delete(page);
                }
                if (repoService.getSiteRepo().findByUrl(rootSiteUrl).isPresent()) {
                    var response = IndexingUtils.getResponse(url);
                    Page Page = new Page(
                            repoService.getSiteRepo().findByUrl(rootSiteUrl).get(),
                            IndexingUtils.getPathOf(url),
                            response.statusCode(),
                            response.parse().toString()
                    );
                    pageRepo.save(Page);
                    lemmatizationService.getAndSaveLemmasAndIndexes(Page);
                    System.out.printf("СТРАНИЦА %s УСПЕШНО ИНДЕКСИРОВАНА\n", Page.getPath());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static boolean isIndexing() {
        return IS_INDEXING.get();
    }
    private void deleteAllDuplicates() {
        var lemmaStringList = lemmaRepo.findAllDoubleLemmasStringList();
        for (String lemma : lemmaStringList) {
            int resultFrequency = 0;
            var indexList = indexRepo.findAllByLemmaString(lemma);
            indexRepo.deleteAll(indexList);
            var DBLemmaList = lemmaRepo.findAllByLemma(lemma);
            var firstDBLemma = DBLemmaList.get(0);
            for (int i = 0; i < DBLemmaList.size(); i++) {
                var DBLemma = DBLemmaList.get(i);
                resultFrequency += DBLemma.getFrequency();
                if (i > 0) {
                    lemmaRepo.delete(DBLemma);
                }
            }
            firstDBLemma.setFrequency(firstDBLemma.getFrequency() + resultFrequency);
            lemmaRepo.save(firstDBLemma);
            indexList.forEach(index -> {
                var newIndex = new SearchIndex(index.getPage(),firstDBLemma,index.getRanking());
                indexRepo.save(newIndex);
            });
        }
    }
}