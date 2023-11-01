package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.RepoService;
import searchengine.services.StatisticsService;
import searchengine.services.indexing.IndexingService;

import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService service;
    private final RepoService repoService;
    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService service, RepoService repoService) {
        this.statisticsService = statisticsService;
        this.service = service;
        this.repoService = repoService;
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
            service.startIndexing();
            response.put("result", "true");
        }
        return response;
    }

    @GetMapping("/stopIndexing")
    public HashMap<String, String> stopIndexing() {
        HashMap<String, String> response = new HashMap<>();
        service.stopAllSites();
        if (IndexingService.isIndexing()) {
            response.put("result", "true");

        } else {
            response.put("result", "false");
            response.put("error", "Индексация не запущена");
        }
        return response;
    }
    @PostMapping("/indexPage")
    public HashMap<String,String> indexPage (@RequestParam String url) {

        HashMap<String,String> response = new HashMap<>();
        boolean isOk = service.indexPage(url);
        if (isOk) {
            response.put("result","true");
        } else {
            response.put("result","false");
            response.put("error","Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        return response;
    }
}