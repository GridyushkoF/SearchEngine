package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
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
    private final SearchIndexRepository indexRepository;
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
    public Optional<Page> createPageByUrlAndGet(String url, String rootSiteUrl) throws IOException {
        Connection.Response response = IndexingUtil.getResponse(url);
        Page page = null;
        if (response != null) {
            page = new Page(
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
            Optional<Site> pageSiteOptional = siteRepository.findByUrl(IndexingUtil.getSiteUrlByPageUrl(url));
            if (pageSiteOptional.isEmpty()) {
                return;
            }
            Optional<Page> pageOptional = pageRepository.findByPathAndSite(IndexingUtil.getPathOf(url), pageSiteOptional.get());
            if (pageOptional.isPresent()) {
                Page page = pageOptional.get();
                List<SearchIndex> indexesByPage = indexRepository.findAllByPage(page);
                indexesByPage.forEach(this::deleteIndexAndAttachedLemmasAndMergeDuplicatesIfExists);
                pageRepository.delete(page);
            }
        } catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS, e.getMessage(), e);
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deleteIndexAndAttachedLemmasAndMergeDuplicatesIfExists(SearchIndex index) {
        List<Lemma> lemmaList = lemmaRepository.findAllByLemma(index.getLemma().getLemma());
        if (!lemmaList.isEmpty()) {
            if (lemmaList.size() > 1) {
                duplicateFixService.mergeAllDuplicates();
            }
            Lemma lemma = lemmaList.get(0);
            lemma.setFrequency(lemma.getFrequency() - 1);
        }
        indexRepository.delete(index);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void markAllSitesAsStoppedByUserAndSave() {
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus() != SiteStatus.INDEXED) {
                site.setStatus(SiteStatus.FAILED);
                site.setLastError("Индексация остановлена пользователем!");
                siteRepository.save(site);
            }
        }
    }
}
