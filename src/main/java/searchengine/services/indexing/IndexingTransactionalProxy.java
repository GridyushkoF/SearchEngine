package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.DuplicateFixService;
import searchengine.services.lemmas.LemmaService;
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
    private final LemmaService lemmaService;
    private final DuplicateFixService duplicateFixService;
    private final List<ForkJoinPool> forkJoinPoolList = new ArrayList<>();

    @Transactional(propagation = Propagation.REQUIRED)
    public void addStopIndexingListener(RecursiveSite recursiveSite) {
        ForkJoinPool pool = ForkJoinPooUtil.createUniqueForkJoinPool();
        pool.invoke(recursiveSite);
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

    @Transactional(propagation = Propagation.REQUIRED)
    public void prepareToStarting() {
        RecursiveSite.clearVisitedLinks();
        IndexingService.IS_INDEXING.set(true);
        forkJoinPoolList.clear();
        indexRepository.deleteAllInBatch();
        indexRepository.deleteAll();
        lemmaRepository.deleteAllInBatch();
        lemmaRepository.deleteAll();
        pageRepository.deleteAllInBatch();
        pageRepository.deleteAll();
        siteRepository.deleteAllInBatch();
        siteRepository.deleteAll();
    }

    public void shutDownAndClearForkJoinPoolList() {
        forkJoinPoolList.forEach(ForkJoinPool::shutdown);
        forkJoinPoolList.clear();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void createPageByUrl(String url, String rootSiteUrl) throws IOException {
        Connection.Response response = IndexingUtil.getResponse(url);
        assert response != null;
        Page page = new Page(
                siteRepository.findByUrl(rootSiteUrl).get(),
                IndexingUtil.getPathOf(url),
                response.statusCode(),
                response.parse().toString(),
                PageStatus.INDEXED
        );
        pageRepository.save(page);
        lemmaService.getAndSaveLemmasAndIndexes(page, true);
        log.info(LogMarkersUtil.INFO, page.getPath() + " successfully reindex");
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deletePageByUrl(String url) {
        Optional<Page> pageOptional = pageRepository.findByPath(IndexingUtil.getPathOf(url));
        if (pageOptional.isPresent()) {
            Page page = pageOptional.get();
            List<SearchIndex> indexList = indexRepository.findAllByPage(page);
            indexList.forEach(indexModel -> {
                List<Lemma> lemmaList = lemmaRepository.findAllByLemma(indexModel.getLemma().getLemma());
                if (!lemmaList.isEmpty()) {
                    if (lemmaList.size() > 1) {
                        duplicateFixService.mergeAllDuplicates();
                    }
                    Lemma lemma = lemmaList.get(0);
                    lemma.setFrequency(lemma.getFrequency() - 1);
                }
                indexRepository.delete(indexModel);
            });
            pageRepository.delete(page);
        }
    }
}
