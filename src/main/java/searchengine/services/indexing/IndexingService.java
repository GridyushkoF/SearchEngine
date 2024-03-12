package searchengine.services.indexing;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.converters.ConfigSiteConverter;
import searchengine.model.*;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.DuplicateFixService;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.IndexingUtils;
import searchengine.util.LogMarkers;
import searchengine.util.MyForkJoinPoolThreadFactory;

import java.io.IOException;
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
    private final LemmaService lemmaService;
    private final DuplicateFixService duplicateFixService;
    private final EntityManager entityManager;

    public static boolean isIndexing() {
        return IS_INDEXING.get();
    }
    @Transactional
    public void prepareToStarting() {
        RecursiveSite.clearVisitedLinks();
        IS_INDEXING.set(true);
        forkJoinPoolList.clear();
        entityManager.clear();
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
    }

    @Transactional
    public void startIndexing() {

        if (!IS_INDEXING.get()) {
            prepareToStarting();
            configSiteList.forEach(configSite -> {
                Site siteEntity = ConfigSiteConverter.getSiteEntityByConfigSite(configSite);
                siteRepository.save(siteEntity);
                NodeLink currentSiteNodeLink = ConfigSiteConverter.getNodeLinkByConfigSite(configSite);
                RecursiveSite recursiveSite = new RecursiveSite(currentSiteNodeLink, siteEntity, lemmaService, pageRepository, lemmaRepository, siteRepository, indexRepository);
                new Thread(() -> addStopIndexingListener(recursiveSite)).start();
            });
        }
    }
    
    private void waitUntilForkJoinPoolEndsWork(ForkJoinPool pool) {
        while (pool.getActiveThreadCount() > 0) {
            
        }
    }
    @Async
    @Transactional
    public void addStopIndexingListener(RecursiveSite recursiveSite) {
        ForkJoinPool pool = MyForkJoinPoolThreadFactory.createUniqueForkJoinPool();
        pool.invoke(recursiveSite);
        forkJoinPoolList.add(pool);
        waitUntilForkJoinPoolEndsWork(pool);
        if(!IndexingService.isIndexing()) {return;}
        pool.shutdown();
        if(recursiveSite.getRootSite().getStatus() != SiteStatus.FAILED) {
            saveSiteUntilStatusNotEqualsGiven(recursiveSite.getRootSite(),SiteStatus.INDEXED);
        }
        duplicateFixService.mergeAllDuplicates();

    }

    private void saveSiteUntilStatusNotEqualsGiven(Site site, SiteStatus givenStatus) {
        while (!site.getStatus().equals(givenStatus)) {
            Site currentRecursiveSite = site;
            currentRecursiveSite.setStatus(givenStatus);
            currentRecursiveSite.setStatusTime(LocalDateTime.now());
            if(givenStatus == SiteStatus.FAILED) {
                currentRecursiveSite.setLastError("Индексация остановлена пользователем!");
            }
            site = siteRepository.save(currentRecursiveSite);
        }
        log.info(LogMarkers.INFO, "Indexing stopped by user or 100% ended: " + site.getName());
    }

    @Transactional
    public  void stopAllSitesByUser() {
        if (!isIndexing()) {
            return;
        }
        IS_INDEXING.set(false);
        forkJoinPoolList.forEach(ForkJoinPool::shutdown);
        try {
            Thread.sleep(2000);
        }catch (Exception e) {
            log.error(e);
        }
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus() != SiteStatus.INDEXED) {
                saveSiteUntilStatusNotEqualsGiven(site,SiteStatus.FAILED);
            }
        }
        duplicateFixService.mergeAllDuplicates();
    }

    @Transactional
    public boolean reindexPage(String url) {
        try {
            String rootSiteUrl = IndexingUtils.getSiteUrlByPageUrl(configSiteList,url);
            boolean isPageInSitesRange = rootSiteUrl != null;
            if (isPageInSitesRange) {
                deletePageByUrl(url);
                if (siteRepository.findByUrl(rootSiteUrl).isPresent()) {
                    createPageByUrl(url, rootSiteUrl);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error(LogMarkers.EXCEPTIONS, "Exception while creating page reindexing: " + url, e);
        }
        return false;
    }

    private void createPageByUrl(String url, String rootSiteUrl) throws IOException {
        Connection.Response response = IndexingUtils.getResponse(url);
        assert response != null;
        Page page = new Page(
                siteRepository.findByUrl(rootSiteUrl).get(),
                IndexingUtils.getPathOf(url),
                response.statusCode(),
                response.parse().toString(),
                PageStatus.INDEXED
        );
        pageRepository.save(page);
        lemmaService.getAndSaveLemmasAndIndexes(page,true);
        log.info(LogMarkers.INFO, page.getPath() + " successfully reindex");
    }

    private void deletePageByUrl(String url) {
        Optional<Page> pageOptional = pageRepository.findByPath(IndexingUtils.getPathOf(url));
        if (pageOptional.isPresent()) {
            Page page = pageOptional.get();
            List<SearchIndex> indexList = indexRepository.findAllByPage(page);
            indexList.forEach(indexModel -> {
                List<Lemma> lemmaList = lemmaRepository.findAllByLemma(indexModel.getLemma().getLemma());
                if (!lemmaList.isEmpty()) {
                    if(lemmaList.size() > 1) {duplicateFixService.mergeAllDuplicates();}
                    Lemma lemma = lemmaList.get(0);
                    lemma.setFrequency(lemma.getFrequency() - 1);
                }
                indexRepository.delete(indexModel);
            });
            pageRepository.delete(page);
        }
    }
}