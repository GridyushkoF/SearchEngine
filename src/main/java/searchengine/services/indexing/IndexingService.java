package searchengine.services.indexing;

import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
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
import searchengine.util.IndexingUtils;
import searchengine.util.LogMarkers;
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
@Component
@Log4j2
public class IndexingService {
    private static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final List<ConfigSite> configSiteList;
    private final List<ForkJoinPool> forkJoinPoolList = new ArrayList<>();
    private ExecutorService executorService;

    private final LemmatizationService lemmatizationService;

    @Autowired
    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, SearchIndexRepository indexRepository, LemmatizationService lemmatizationService) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
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
            indexRepository.deleteAll();
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
                RecursiveSite recursiveSite = new RecursiveSite(currentSiteNodeLink, site, lemmatizationService, pageRepository, lemmaRepository, siteRepository, indexRepository);
                addStopIndexingListener(recursiveSite);
            }));
        }
    }

    @Async
    public void addStopIndexingListener(RecursiveSite recursiveSite) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors(),
                new MyFjpThreadFactory(),
                (t, e) -> log.error(LogMarkers.EXCEPTIONS, "Exception while creating MyFjpThreadFactory()", e),
                true
        );
        forkJoinPool.invoke(recursiveSite);
        forkJoinPoolList.add(forkJoinPool);
        while (forkJoinPool.getActiveThreadCount() > 0) {
            //wait
        }
        deleteAllDuplicates();
        IS_INDEXING.set(false);
        Site curWalkerRootSite = recursiveSite.getRootSite();
        curWalkerRootSite.setStatus("INDEXED");
        curWalkerRootSite.setStatusTime(LocalDateTime.now());
        log.info(LogMarkers.INFO, curWalkerRootSite.getName() + " successfully graduated the indexing");
        executorService.shutdownNow();

    }

    @Transactional
    public void stopAllSitesByUser() {
        deleteAllDuplicates();
        IS_INDEXING.set(false);
        executorService.shutdownNow();
        siteRepository.findAll().forEach(siteModel -> {
            siteModel.setStatus("FAILED");
            siteModel.setStatusTime(LocalDateTime.now());
            siteModel.setLastError("Индексация остановлена пользователем!");
            siteRepository.save(siteModel);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.info(LogMarkers.INFO, siteRepository.findByUrl(siteModel.getUrl()) + " stopped by user");
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
                if (IndexingUtils.compareHosts(url, configSite.getUrl())) {
                    isPageInSitesRange = true;
                    rootSiteUrl = configSite.getUrl();
                    break;
                }
            }
            if (isPageInSitesRange) {
                Optional<Page> pageOPT = pageRepository.findByPath(IndexingUtils.getPathOf(url));
                if (pageOPT.isPresent()) {
                    Page page = pageOPT.get();
                    List<SearchIndex> indexList = indexRepository.findAllByPage(page);
                    indexList.forEach(indexModel -> {
                        List<Lemma> lemmaList = lemmaRepository.findAllByLemma(indexModel.getLemma().getLemma());
                        if (lemmaList.size() > 0) {
                            Lemma lemma = lemmaList.get(0);
                            lemma.setFrequency(lemma.getFrequency() - 1);
                        }
                        indexRepository.delete(indexModel);
                    });
                    pageRepository.delete(page);
                }
                if (siteRepository.findByUrl(rootSiteUrl).isPresent()) {
                    Connection.Response response = IndexingUtils.getResponse(url);
                    assert response != null;
                    Page Page = new Page(
                            siteRepository.findByUrl(rootSiteUrl).get(),
                            IndexingUtils.getPathOf(url),
                            response.statusCode(),
                            response.parse().toString()
                    );
                    pageRepository.save(Page);
                    lemmatizationService.getAndSaveLemmasAndIndexes(Page);
                    log.info(LogMarkers.INFO, Page.getPath() + " successfully reindex");
                    return true;
                }
            }
        } catch (Exception e) {
            log.error(LogMarkers.EXCEPTIONS, "Exception while creating page reindexing: " + url, e);
        }
        return false;
    }

    private void deleteAllDuplicates() {
        List<String> lemmaStringList = lemmaRepository.findAllDoubleLemmasStringList();
        for (String lemma : lemmaStringList) {
            int resultFrequency = 0;
            List<SearchIndex> indexList = indexRepository.findAllByLemmaString(lemma);
            indexRepository.deleteAll(indexList);
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
                indexRepository.save(newIndex);
            });
        }
    }
}