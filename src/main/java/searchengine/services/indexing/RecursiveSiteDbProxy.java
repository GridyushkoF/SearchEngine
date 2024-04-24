package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.LogMarkersUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Log4j2
public class RecursiveSiteDbProxy {
    private final SiteRepository siteRepository;
    private final LemmaService lemmaService;
    private final PageRepository pageRepository;
    private RecursiveSiteDbProxy selfProxy;
    private final AtomicReference<LocalDateTime> prevTimeOfSiteUpdate = new AtomicReference<>(LocalDateTime.now());

    @Transactional(noRollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public void setCurrentTimeToRootSite(Site rootSite) {
        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(prevTimeOfSiteUpdate.get(), now).getSeconds() < 5) {
            return;
        }
        try {
            rootSite.setStatusTime(now);
            synchronized (siteRepository) {
                siteRepository.save(rootSite);
            }
            prevTimeOfSiteUpdate.set(now);
        } catch (Throwable e) {
            log.error(LogMarkersUtil.EXCEPTIONS, e, e.getCause());
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(maxAttempts = 50, exceptionExpression = "#exception instanceof T(org.springframework.dao.BatchUpdateException)")
    public void saveAndClearTempPages(NodeLink currentNodeLink, Set<Page> tempPages) {
        pageRepository.saveAll(tempPages);
        log.info(LogMarkersUtil.INFO, "PAGES INIT: " + currentNodeLink.getLink() + "\n\t" + tempPages);
        tempPages.forEach(page -> lemmaService.getAndSaveLemmasAndIndexes(page, false));
        tempPages.clear();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Recover
    public void saveAndClearTempPages(Throwable throwable, NodeLink currentNodeLink, Set<Page> tempPages) {
        synchronized (pageRepository) {
            log.error("Не удалось сохранить страницы (вероятно - дедлок), выполняется попытка перезапуска транзакции в безопасном режиме");
            selfProxy.saveAndClearTempPages(currentNodeLink,tempPages);
        }
    }

    @Autowired
    public void setSelfProxy(@Lazy RecursiveSiteDbProxy selfProxy) {
        this.selfProxy = selfProxy;
    }
}
