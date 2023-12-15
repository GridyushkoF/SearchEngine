package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.RepoService;
import searchengine.services.StatisticsService;
import searchengine.services.indexing.IndexingService;
import searchengine.services.indexing.SearchService;

import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService service, RepoService repoService) {
        this.statisticsService = statisticsService;
        this.indexingService = service;
        this.searchService = new SearchService(repoService);
    }

    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     */
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
        indexingService.stopAllSites();
        if (IndexingService.isIndexing()) {
            response.put("result", "true");

        } else {
            response.put("result", "false");
            response.put("error", "Индексация не запущена");
        }
        return response;
    }
    @PostMapping("/indexPage")
    public HashMap<String,String> indexPage
            (@RequestParam String url) {

        HashMap<String,String> response = new HashMap<>();
        boolean isOk = indexingService.indexPage(url);
        if (isOk) {
            response.put("result","true");
        } else {
            response.put("result","false");
            response.put("error","Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        return response;
    }
    @GetMapping("search")
    public HashMap<String,String> search
            (@RequestParam String query,
             @RequestParam(value = "siteUrl",required = false) String siteUrl,
             @RequestParam(value = "offset",required = false,defaultValue = "0") int offset,
             @RequestParam(value = "limit",required = false,defaultValue = "20") int limit)

    {
        var searchResults = searchService.search(query, siteUrl);
        System.out.println(searchResults.subList(offset,limit < searchResults.size() ? limit : searchResults.size()));
        return null;
    }
}