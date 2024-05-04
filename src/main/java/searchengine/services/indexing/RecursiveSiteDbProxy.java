package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
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
    private final PageRepository pageRepository;
    private RecursiveSiteDbProxy selfProxy;
    private final AtomicReference<LocalDateTime> prevTimeOfSiteUpdate = new AtomicReference<>(LocalDateTime.now());

    public void setCurrentTimeToRootSite(SiteEntity rootSite) {
        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(prevTimeOfSiteUpdate.get(), now).getSeconds() < 5) {
            return;
        }
        rootSite.setStatusTime(now);
        siteRepository.save(rootSite);
        prevTimeOfSiteUpdate.set(now);
    }

    @Transactional(propagation = Propagation.REQUIRED,isolation = Isolation.REPEATABLE_READ)
    public void saveTempPages(NodeLink currentNodeLink, Set<PageEntity> tempPages) {
        pageRepository.saveAllAndFlush(tempPages);
        log.info(LogMarkersUtil.INFO, "PAGES INIT: " + currentNodeLink.getLink() + "\n\t" + tempPages);
    }


    @Autowired
    public void setProxy(@Lazy RecursiveSiteDbProxy selfProxy) {
        this.selfProxy = selfProxy;
    }
}
