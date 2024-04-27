package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.converters.ConfigSiteConverter;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.DuplicateFixService;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.IndexingUtil;
import searchengine.util.LogMarkersUtil;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Log4j2
@RequiredArgsConstructor
public class IndexingService {
    public static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);
    private final YamlParser yamlParser = new YamlParser();
    private final List<ConfigSite> configSiteList = yamlParser.getSitesFromYaml();
    private final IndexingTransactionalProxy indexingTransactionalProxy;
    private final SiteRepository siteRepository;
    private final RecursiveSiteDbProxy recursiveSiteDbProxy;
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
                log.info(LogMarkersUtil.INFO, "ConfigSite: " + configSite);
                SiteEntity siteEntity = ConfigSiteConverter.getSiteEntityByConfigSite(configSite);
                siteRepository.save(siteEntity);
                NodeLink currentSiteNodeLink = ConfigSiteConverter.getNodeLinkByConfigSite(configSite);
                RecursiveSite recursiveSite = new RecursiveSite(currentSiteNodeLink, siteEntity,recursiveSiteDbProxy,lemmaService  );
                new Thread(() -> indexingTransactionalProxy.addStopIndexingListener(recursiveSite)).start();
            });
        }
    }

    @Transactional
    public void stopAllSitesAndMergeAllDuplicates() {
        if (!isIndexing()) {
            return;
        }
        IS_INDEXING.set(false);
        indexingTransactionalProxy.shutDownAndClearForkJoinPoolList();
        indexingTransactionalProxy.markAllSitesAsStoppedByUserAndSave();
        duplicateFixService.mergeAllDuplicates();
    }

    @Transactional
    public boolean reindexPageByUrl(String url) {
        try {
            if (reindexPageAndGetStatus(url)) return true;
        } catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS, "Exception while creating page reindexing: " + url, e);
            return false;
        }
        return false;
    }
    @Transactional
    public boolean reindexPageAndGetStatus(String url) throws Exception {
        String rootSiteUrl = IndexingUtil.getSiteUrlByPageUrl(url);
        boolean isPageInSitesRange = rootSiteUrl != null;
        if (isPageInSitesRange && siteRepository.findByUrl(rootSiteUrl).isPresent()) {
            indexingTransactionalProxy.deletePageByUrlAndMergeDuplicatesIfExists(url);
            Optional<PageEntity> pageOptional = indexingTransactionalProxy.createPageByUrlAndGet(url, rootSiteUrl);
            pageOptional.ifPresent(page -> lemmaService.getAndSaveLemmasAndIndexes(page, true));
            return true;
        }
        return false;
    }
}
