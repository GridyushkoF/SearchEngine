package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import searchengine.services.StatisticsService;
import searchengine.services.indexing.IndexingService;

import java.time.LocalDateTime;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private boolean isIndexing = false;
    @Autowired
    public ApiController(StatisticsService statisticsService, SiteRepository siteRepository, PageRepository pageRepository) {
        this.statisticsService = statisticsService;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.service = new IndexingService(siteRepository, pageRepository);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexingService service;

    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     */
    @GetMapping("/startIndexing")
    public  HashMap<String, String> startIndexing() {
        HashMap<String, String> response = new HashMap<>();
        if (!isIndexing) {
            service.startIndexing();
            isIndexing = true;
            response.put("result", "true");
        } else {
            response.put("result", "false");
            response.put("error", "Индексация уже запущена");

        }
        return response;
    }
    @GetMapping("/stopIndexing")
    public  HashMap<String, String> stopIndexing() {

        HashMap<String, String> response = new HashMap<>();
        siteRepository.findAll().forEach(site -> {
           if (site.isIndexing())
           {
               service.stopSite(site);
           }
       });
       if (isIndexing) {
           isIndexing = false;
           response.put("result","true");
       } else  {
           response.put("result","false");
           response.put("error","Индексация не запущена");
       }
       return response;
    }

}
