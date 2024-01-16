package searchengine.services.indexing;

import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.LemmatizationService;
import searchengine.util.IndexingUtil;
import searchengine.util.MyFjpThreadFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Log4j2
public class IndexingService {
    private static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepo;
    private final List<ConfigSite> configSiteList;
    private final List<ForkJoinPool> forkJoinPoolList = new ArrayList<>();
    private final LemmatizationService lemmatizationService;
    private ExecutorService executorService;

    @Autowired
    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, SearchIndexRepository indexRepo, LemmatizationService lemmatizationService) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepo = indexRepo;
        this.lemmatizationService = lemmatizationService;
        this.configSiteList = YamlParser.getSitesFromYaml();
    }

    public static boolean isIndexing() {
        return IS_INDEXING.get();
    }

    public void startIndexing() {
        if (!IS_INDEXING.get()) {
            RecursiveSite.clearVisitedLinks();
            IS_INDEXING.set(true);
            indexRepo.deleteAll();
            lemmaRepository.deleteAll();
            pageRepository.deleteAll();
            siteRepository.deleteAll();
            executorService = Executors.newFixedThreadPool(configSiteList.size());
            configSiteList.forEach(configSite -> executorService.submit(() -> {
                Site site = new Site(
                        "INDEXING",
                        LocalDateTime.now(),
                        null,
                        configSite.getUrl(),
                        configSite.getName());
                siteRepository.save(site);
                NodeLink currentSiteNodeLink = new NodeLink(
                        configSite.getUrl(),
                        configSite.getUrl()
                );
                RecursiveSite recursiveSite = new RecursiveSite(
                        currentSiteNodeLink,
                        site,
                        lemmatizationService,
                        pageRepository,
                        lemmaRepository,
                        siteRepository,
                        indexRepo);
                addStopIndexingListener(recursiveSite);
            }));
        }
    }

    @Async
    public void addStopIndexingListener(RecursiveSite site) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors(),
                new MyFjpThreadFactory(),
                (t, e) -> log.error("Exception while adding the event listener to RecursiveSite: " + site.getCurrentNodeLink().getLink(), e),
                true
        );
        forkJoinPool.invoke(site);
        forkJoinPoolList.add(forkJoinPool);
        while (forkJoinPool.getActiveThreadCount() > 0) {
            //wait
        }
        deleteAllDuplicates();
        IS_INDEXING.set(false);
        Site rootSiteOfCurrentSite = site.getRootSite();
        rootSiteOfCurrentSite.setStatus("INDEXED");
        rootSiteOfCurrentSite.setStatusTime(LocalDateTime.now());
        log.info(rootSiteOfCurrentSite.getName() + " successfully graduated the indexing");
        executorService.shutdownNow();

    }

    @Transactional
    public void stopAllSitesByUser() {
        deleteAllDuplicates();
        IS_INDEXING.set(false);
        executorService.shutdownNow();
        siteRepository.findAll().forEach(SiteModel -> {
            SiteModel.setStatus("FAILED");
            SiteModel.setStatusTime(LocalDateTime.now());
            SiteModel.setLastError("Индексация остановлена пользователем!");
            siteRepository.save(SiteModel);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("Sleep exception", e);
            }
            log.info(siteRepository.findByUrl(SiteModel.getUrl()) + " stopped by user");
            System.out.println();
        });
        forkJoinPoolList.forEach(ForkJoinPool::shutdown);
    }

    @Transactional
    public boolean reindexPage(String url) {
        boolean isPageInSitesRange = false;
        String rootSiteUrl = "";
        try {
            for (ConfigSite configSite : configSiteList) {
                if (IndexingUtil.compareHosts(url, configSite.getUrl())) {
                    isPageInSitesRange = true;
                    rootSiteUrl = configSite.getUrl();
                    break;
                }
            }
            if (isPageInSitesRange) {
                Optional<Page> pageOPT = pageRepository.findByPath(IndexingUtil.getPathOf(url));
                if (pageOPT.isPresent()) {
                    Page page = pageOPT.get();
                    List<SearchIndex> indexes = indexRepo.findAllByPage(page);
                    indexes.forEach(indexModel -> {
                        List<Lemma> lemmaList = lemmaRepository.findAllByLemma(indexModel.getLemma().getLemma());
                        if (lemmaList.size() > 0) {
                            Lemma lemma = lemmaList.get(0);
                            lemma.setFrequency(lemma.getFrequency() - 1);
                        }
                        indexRepo.delete(indexModel);
                    });
                    pageRepository.delete(page);
                }
                if (siteRepository.findByUrl(rootSiteUrl).isPresent()) {
                    Connection.Response response = IndexingUtil.getResponse(url);
                    assert response != null;
                    Page Page = new Page(
                            siteRepository.findByUrl(rootSiteUrl).get(),
                            IndexingUtil.getPathOf(url),
                            response.statusCode(),
                            response.parse().toString()
                    );
                    pageRepository.save(Page);
                    lemmatizationService.getAndSaveLemmasAndIndexes(Page);
                    log.info(Page.getPath() + " successfully reindex");
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Exception while page reindexing: " + url);
        }
        return false;
    }

    private void deleteAllDuplicates() {
        List<String> lemmaStringList = lemmaRepository.findAllDoubleLemmasStringList();
        for (String lemma : lemmaStringList) {
            int resultFrequency = 0;
            List<SearchIndex> indexList = indexRepo.findAllByLemmaString(lemma);
            indexRepo.deleteAll(indexList);
            List<Lemma> lemmaModelList = lemmaRepository.findAllByLemma(lemma);
            Lemma firstLemmaModel = lemmaModelList.get(0);
            for (int i = 0; i < lemmaModelList.size(); i++) {
                Lemma lemmaModel = lemmaModelList.get(i);
                resultFrequency += lemmaModel.getFrequency();
                if (i > 0) {
                    lemmaRepository.delete(lemmaModel);
                }
            }
            firstLemmaModel.setFrequency(firstLemmaModel.getFrequency() + resultFrequency);
            lemmaRepository.save(firstLemmaModel);
            indexList.forEach(index -> {
                SearchIndex newIndex = new SearchIndex(index.getPage(), firstLemmaModel, index.getRanking());
                indexRepo.save(newIndex);
            });
        }
    }
}