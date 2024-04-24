package searchengine.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.search.CorrectSearchResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResult;
import searchengine.dto.search.UncorrectSearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StatisticsService;
import searchengine.services.indexing.IndexingService;
import searchengine.services.searching.MainSearchService;
import searchengine.util.LogMarkersUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Log4j2
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final MainSearchService mainSearchService;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService service, MainSearchService mainSearchService) {
        this.statisticsService = statisticsService;
        this.indexingService = service;
        this.mainSearchService = mainSearchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public HashMap<String, String> startIndexing() {
        HashMap<String, String> response = new HashMap<>();
        if (IndexingService.isIndexing()) {
            response.put("result", "false");
            response.put("error", "Индексация уже запущена");
        } else {
            indexingService.startIndexing();
            response.put("result", "true");
        }
        return response;
    }

    @GetMapping("/stopIndexing")
    public HashMap<String, String> stopIndexing() {
        HashMap<String, String> response = new HashMap<>();
        if (IndexingService.isIndexing()) {
            response.put("result", "true");

        } else {
            response.put("result", "false");
            response.put("error", "Индексация не запущена");
        }
        indexingService.stopAllSitesAndMergeAllDuplicates();
        return response;
    }

    @PostMapping("/indexPage")
    public HashMap<String, String> indexPage
            (@RequestParam String url) {
        HashMap<String, String> response = new HashMap<>();
        boolean isOk = indexingService.reindexPageByUrl(url);
        log.error(LogMarkersUtil.EXCEPTIONS, "Страница:\n\t" + url + "\nСтатус:" + isOk);
        if (isOk) {
            response.put("result", "true");
        } else {
            response.put("result", "false");
            response.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        return response;
    }

    @GetMapping("search")
    public ResponseEntity<SearchResponse> search
            (@RequestParam String query,
             @RequestParam(value = "siteUrl", required = false) String siteUrl,
             @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
             @RequestParam(value = "limit", required = false, defaultValue = "1000000") int limit) {
        if (query.isEmpty()) {
            return ResponseEntity.ok(new UncorrectSearchResponse("Задан пустой поисковой запрос!"));
        }
        Optional<List<SearchResult>> searchResultsOptional = mainSearchService.searchByQuery(query, siteUrl);
        if (searchResultsOptional.isEmpty()) {
            return ResponseEntity.ok(new UncorrectSearchResponse("По данному запросу ничего не найдено("));
        }
        List<SearchResult> searchResults = searchResultsOptional.get();
        int searchResultsAmount = searchResults.size();
        searchResults = searchResults.subList(offset, Math.min(offset + limit, searchResultsAmount));
        return ResponseEntity.ok(new CorrectSearchResponse(
                searchResults,
                true,
                searchResultsAmount
        ));
    }
}