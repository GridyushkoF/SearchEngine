package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConfigSite;
import searchengine.config.YamlParser;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.IndexingService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final YamlParser yamlParser;

    @Override
    @Transactional
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(yamlParser.getSitesFromYaml().size());
        total.setIndexing(IndexingService.isIndexing());
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<ConfigSite> sitesList = yamlParser.getSitesFromYaml();
        for (ConfigSite configSite : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(configSite.getName());
            item.setUrl(configSite.getUrl());
            item.setError("Ошибки остутствуют!");
            Optional<SiteEntity> siteOptional = siteRepository.findByUrl(configSite.getUrl());
            siteOptional.ifPresent(site -> {
                item.setStatus(site.getStatus().name());
                item.setError(site.getLastError());
                ZonedDateTime zdt = ZonedDateTime.of(site.getStatusTime(), ZoneId.systemDefault());
                item.setStatusTime(zdt.toInstant().toEpochMilli());
                int pagesAmount = pageRepository.countBySite(site);
                int lemmasAmount = lemmaRepository.countBySite(site);
                item.setPages(pagesAmount);
                item.setLemmas(lemmasAmount);
            });
            total.setPages((int) pageRepository.count());
            total.setLemmas((int) lemmaRepository.count());
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
