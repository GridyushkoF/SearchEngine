package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemmas.LemmaService;
import searchengine.util.LogMarkersUtil;

import java.time.LocalDateTime;
import java.util.Set;

@RequiredArgsConstructor
@Log4j2
@Service
public class RecursiveSiteTransactionalProxy {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;
    @Transactional
    public void setCurrentTimeToRootSite(Site rootSite) {
        rootSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(rootSite);
    }
    @Transactional
    public void saveAndClearTempPages(NodeLink currentNodeLink, Set<Page> tempPages) {
        pageRepository.saveAll(tempPages);
        log.info(LogMarkersUtil.INFO, "PAGE INIT: " + currentNodeLink.getLink());
        tempPages.forEach(page -> lemmaService.getAndSaveLemmasAndIndexes(page, false));
        tempPages.clear();
    }
}
