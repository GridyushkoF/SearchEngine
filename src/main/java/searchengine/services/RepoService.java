package searchengine.services;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaRepository;
import searchengine.model.PageRepository;
import searchengine.model.SearchIndexRepository;
import searchengine.model.SiteRepository;
@Service
@Data
public class RepoService {
    private final LemmaRepository lemmaRepo;
    private final PageRepository pageRepo;
    private final SearchIndexRepository indexRepo;
    private final SiteRepository siteRepo;
    @Autowired
    public RepoService(LemmaRepository lemmaRepo, PageRepository pageRepo, SearchIndexRepository indexRepo, SiteRepository siteRepo) {
        this.lemmaRepo = lemmaRepo;
        this.pageRepo = pageRepo;
        this.indexRepo = indexRepo;
        this.siteRepo = siteRepo;
    }
}
