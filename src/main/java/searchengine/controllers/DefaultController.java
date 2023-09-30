package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import searchengine.services.indexing.IndexingService;

@Controller
public class DefaultController {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     */
    @RequestMapping("/api/startIndexing")
    public void startIndexing() {
        IndexingService service = new IndexingService(siteRepository,pageRepository);
        service.startIndexing();
    }
    @RequestMapping
    private String index ()
    {
        return "index";
    }
}
