package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@AllArgsConstructor
public class IndexingService {
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    public void startIndexing ()
    {
        List<ConfigSite> configSiteList = YamlParser.getSitesFromYaml();
        pageRepo.deleteAll();
        siteRepo.deleteAll();
        ForkJoinPool pool = new ForkJoinPool();
        configSiteList.forEach(configSite -> {
            new Thread(() -> {
                Site site = new Site(
                        "INDEXING",
                        LocalDateTime.now(),
                        null,
                        configSite.getUrl(),
                        configSite.getName());
                siteRepo.save(site);
                //init data:
                NodeLink currentSiteNodeLink = new NodeLink(
                        configSite.getUrl(),
                        configSite.getUrl()
                );
                SiteWalker walker = new SiteWalker(currentSiteNodeLink, site, pageRepo, siteRepo);
                pool.invoke(walker);
                System.out.println("УСПЕШНО ДОБАВЛЕН В ИНДЕКСАЦИЮ САЙТ: " + site.getUrl());
            }).start();
        });
        System.out.println("end");
    }
}