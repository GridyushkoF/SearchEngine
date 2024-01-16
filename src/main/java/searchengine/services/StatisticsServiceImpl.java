package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.IndexingService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    @Autowired
    public StatisticsServiceImpl(LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository) {
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(YamlParser.getSitesFromYaml().size());
        total.setIndexing(IndexingService.isIndexing());
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<ConfigSite> sitesList = YamlParser.getSitesFromYaml();
        for (ConfigSite configSite : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(configSite.getName());
            item.setUrl(configSite.getUrl());
            int pagesAmount = (int) pageRepository.count();
            int lemmasAmount = (int) lemmaRepository.count();
            item.setPages(pagesAmount);
            item.setLemmas(lemmasAmount);
            var optSite = siteRepository.findByUrl(configSite.getUrl());
            optSite.ifPresent(site -> {
                item.setStatus(site.getStatus());
                item.setError(site.getLastError());
                ZonedDateTime zdt = ZonedDateTime.of(site.getStatusTime(), ZoneId.systemDefault());
                item.setStatusTime(zdt.toInstant().toEpochMilli());
            });
            total.setPages(total.getPages() + pagesAmount);
            total.setLemmas(total.getLemmas() + lemmasAmount);
            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
