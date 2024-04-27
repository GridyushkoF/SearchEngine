package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.DuplicateFixService;
import searchengine.util.ForkJoinPooUtil;
import searchengine.util.IndexingUtil;
import searchengine.util.LogMarkersUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Log4j2
public class IndexingTransactionalProxy {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final DuplicateFixService duplicateFixService;
    private final List<ForkJoinPool> forkJoinPoolList = new ArrayList<>();

    @Transactional(propagation = Propagation.REQUIRED)
    public void addStopIndexingListener(RecursiveSite recursiveSite) {
        ForkJoinPool pool = ForkJoinPooUtil.createUniqueForkJoinPool();
        pool.execute(recursiveSite);
        forkJoinPoolList.add(pool);
        waitUntilForkJoinPoolEndsWork(pool);
        if (!IndexingService.isIndexing()) {
            return;
        }
        pool.shutdown();
        if (recursiveSite.getRootSite().getStatus() != SiteStatus.FAILED) {
            recursiveSite.getRootSite().setStatus(SiteStatus.INDEXED);
            siteRepository.save(recursiveSite.getRootSite());
            log.info(LogMarkersUtil.INFO, "Сайт окончил индексирование: " + recursiveSite.getRootSite().getUrl());
        }

    }

    private void waitUntilForkJoinPoolEndsWork(ForkJoinPool pool) {
        while (pool.getActiveThreadCount() > 0) {

        }
    }

    public void prepareToStarting() {
        RecursiveSite.clearVisitedLinks();
        IndexingService.IS_INDEXING.set(true);
        forkJoinPoolList.clear();
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
    }

    public void shutDownAndClearForkJoinPoolList() {
        forkJoinPoolList.forEach(ForkJoinPool::shutdown);
        forkJoinPoolList.clear();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<PageEntity> createPageByUrlAndGet(String url, String rootSiteUrl) throws IOException {
        Connection.Response response = IndexingUtil.getResponse(url);
        PageEntity page = null;
        if (response != null) {
            page = new PageEntity(
                    siteRepository.findByUrl(rootSiteUrl).get(),
                    IndexingUtil.getPathOf(url),
                    response.statusCode(),
                    response.parse().toString(),
                    PageStatus.INDEXED
            );
            pageRepository.save(page);
            log.info(LogMarkersUtil.INFO, page.getPath() + " successfully reindex");
        }
        return page != null ? Optional.of(page) : Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deletePageByUrlAndMergeDuplicatesIfExists(String url) {
        try {
            Optional<SiteEntity> pageSiteOptional = siteRepository.findByUrl(IndexingUtil.getSiteUrlByPageUrl(url));
            if (pageSiteOptional.isEmpty()) {
                return;
            }
            Optional<PageEntity> pageOptional = pageRepository.findByPathAndSite(IndexingUtil.getPathOf(url), pageSiteOptional.get());
            if (pageOptional.isPresent()) {
                PageEntity page = pageOptional.get();
                List<IndexEntity> indexesByPage = indexRepository.findAllByPage(page);
                indexesByPage.forEach(this::deleteIndexAndAttachedLemmasAndMergeDuplicatesIfExists);
                pageRepository.delete(page);
            }
        } catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS, e.getMessage(), e);
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deleteIndexAndAttachedLemmasAndMergeDuplicatesIfExists(IndexEntity index) {
        List<LemmaEntity> lemmaEntityList = lemmaRepository.findAllByLemma(index.getLemmaEntity().getLemma());
        if (!lemmaEntityList.isEmpty()) {
            if (lemmaEntityList.size() > 1) {
                duplicateFixService.mergeAllDuplicates();
            }
            LemmaEntity lemmaEntity = lemmaEntityList.get(0);
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() - 1);
        }
        indexRepository.delete(index);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void markAllSitesAsStoppedByUserAndSave() {
        for (SiteEntity site : siteRepository.findAll()) {
            if (site.getStatus() != SiteStatus.INDEXED) {
                site.setStatus(SiteStatus.FAILED);
                site.setLastError("Индексация остановлена пользователем!");
                siteRepository.save(site);
            }
        }
    }
}
