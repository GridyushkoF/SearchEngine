package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.converters.ConfigSiteConverter;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.DuplicateFixService;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.IndexingUtil;
import searchengine.util.LogMarkersUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Log4j2
@RequiredArgsConstructor
public class IndexingService {
    public static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);
    private final IndexingTransactionalProxy indexingTransactionalProxy;
    private final RecursiveSiteTransactionalProxy recursiveSiteTransactionalProxy;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final List<ConfigSite> configSiteList = YamlParser.getSitesFromYaml();
    private final LemmaService lemmaService;
    private final DuplicateFixService duplicateFixService;

    public static boolean isIndexing() {
        return IS_INDEXING.get();
    }

    @Transactional
    public void startIndexing() {
        if (!IS_INDEXING.get()) {
            indexingTransactionalProxy.prepareToStarting();
            configSiteList.forEach(configSite -> {
                log.info(LogMarkersUtil.INFO,"ConfigSite: " + configSite);
                Site siteEntity = ConfigSiteConverter.getSiteEntityByConfigSite(configSite);
                siteRepository.save(siteEntity);
                NodeLink currentSiteNodeLink = ConfigSiteConverter.getNodeLinkByConfigSite(configSite);
                RecursiveSite recursiveSite = new RecursiveSite(currentSiteNodeLink, siteEntity, lemmaService, pageRepository, lemmaRepository, siteRepository, indexRepository, recursiveSiteTransactionalProxy);
                new Thread(() -> indexingTransactionalProxy.addStopIndexingListener(recursiveSite)).start();
            });

        }
    }
    @Transactional
    public void stopAllSitesByUserAndMergeAllDuplicates() {
        if (!isIndexing()) {
            return;
        }
        IS_INDEXING.set(false);
        indexingTransactionalProxy.shutDownAndClearForkJoinPoolList();
        markAllSitesAsStoppedByUserAndSave();
        duplicateFixService.mergeAllDuplicates();
    }

    private void markAllSitesAsStoppedByUserAndSave() {
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus() != SiteStatus.INDEXED) {
                site.setStatus(SiteStatus.FAILED);
                site.setLastError("Индексация остановлена пользователем!");
                siteRepository.save(site);
            }
        }
    }

    @Transactional
    public boolean reindexPageByUrl(String url) {
        try {
            if (reindexPageAndReturnStatus(url)) return true;
        } catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS, "Exception while creating page reindexing: " + url, e);
            return false;
        }
        return false;
    }

    private boolean reindexPageAndReturnStatus(String url) throws Exception {
        String rootSiteUrl = IndexingUtil.getSiteUrlByPageUrl(configSiteList, url);
        boolean isPageInSitesRange = rootSiteUrl != null;
        if (isPageInSitesRange && siteRepository.findByUrl(rootSiteUrl).isPresent()) {
            indexingTransactionalProxy.deletePageByUrl(url);
            indexingTransactionalProxy.createPageByUrl(url, rootSiteUrl);
            return true;
        }
        return false;
    }

}
