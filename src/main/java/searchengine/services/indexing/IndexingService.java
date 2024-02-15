package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.model.*;
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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Log4j2
@RequiredArgsConstructor
public class IndexingService {
    private static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);
    private final List<ForkJoinPool> forkJoinPoolList = new ArrayList<>();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final List<ConfigSite> configSiteList = YamlParser.getSitesFromYaml();
    private final LemmatizationService lemmatizationService;

    public static boolean isIndexing() {
        return IS_INDEXING.get();
    }
    @Transactional
    public void startIndexing() {
        if (!IS_INDEXING.get()) {
            RecursiveSite.clearVisitedLinks();
            IS_INDEXING.set(true);
            indexRepository.deleteAll();
            lemmaRepository.deleteAll();
            pageRepository.deleteAll();
            siteRepository.deleteAll();
            configSiteList.forEach(configSite -> {
                Site site = new Site(
                        SiteStatus.INDEXING,
                        LocalDateTime.now(),
                        "",
                        configSite.getUrl(),
                        configSite.getName());
                siteRepository.save(site);
                NodeLink currentSiteNodeLink = new NodeLink(
                        configSite.getUrl(),
                        configSite.getUrl()
                );
                RecursiveSite recursiveSite = new RecursiveSite(currentSiteNodeLink, site, lemmatizationService, pageRepository, lemmaRepository, siteRepository, indexRepository);
                new Thread(() -> addStopIndexingListener(recursiveSite)).start();
            });
        }
    }
    @Async
    @Transactional
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
        if(!IndexingService.isIndexing()) {
            return;
        }

        if(siteRepository.findByUrl(recursiveSite.getRootSite().getUrl()).get().getStatus() != SiteStatus.FAILED) {
            IS_INDEXING.set(false);
            deleteAllDuplicates();
            forkJoinPool.shutdown();
            Site savedSite = recursiveSite.getRootSite();
            while (!savedSite.getStatus().equals(SiteStatus.INDEXED)) {
                Site curWalkerRootSite = recursiveSite.getRootSite();
                curWalkerRootSite.setStatus(SiteStatus.INDEXED);
                curWalkerRootSite.setStatusTime(LocalDateTime.now());
                savedSite = siteRepository.save(curWalkerRootSite);
                log.info(LogMarkers.INFO, curWalkerRootSite.getName() + " successfully graduated the indexing");
            }

        }

    }

    @Transactional
    @Async
    public void stopAllSitesByUser() {
        if (!isIndexing()) {
            return;
        }
        IS_INDEXING.set(false);
        forkJoinPoolList.forEach(ForkJoinPool::shutdown);
        deleteAllDuplicates();
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus() != SiteStatus.INDEXED) {
                Site savedSite = site;
                while (!savedSite.getStatus().equals(SiteStatus.FAILED)) {
                    site.setStatus(SiteStatus.FAILED);
                    site.setStatusTime(LocalDateTime.now());
                    site.setLastError("Индексация остановлена пользователем!");
                    savedSite = siteRepository.save(site);
                }
                log.info(LogMarkers.INFO, "ОСТАНОВЛЕНО ПОЛЬЗОВАТЕЛЕМ:" + siteRepository.findByUrl(site.getUrl()));
            }
        }
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
                    Page page = new Page(
                            siteRepository.findByUrl(rootSiteUrl).get(),
                            IndexingUtils.getPathOf(url),
                            response.statusCode(),
                            response.parse().toString(),
                            PageStatus.INIT
                    );
                    pageRepository.save(page);
                    lemmatizationService.getAndSaveLemmasAndIndexes(page,true);
                    log.info(LogMarkers.INFO, page.getPath() + " successfully reindex");
                    return true;
                }
            }
        } catch (Exception e) {
            log.error(LogMarkers.EXCEPTIONS, "Exception while creating page reindexing: " + url, e);
        }
        return false;
    }
    @Transactional
    public void deleteAllDuplicates() {
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