package searchengine.services.other;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.repositories.LemmaRepo;
import searchengine.repositories.PageRepo;
import searchengine.repositories.SearchIndexRepo;
import searchengine.repositories.SiteRepo;
@Service
@Data
public class RepoService {
    private final LemmaRepo lemmaRepo;
    private final PageRepo pageRepo;
    private final SearchIndexRepo indexRepo;
    private final SiteRepo siteRepo;
    @Autowired
    public RepoService(LemmaRepo lemmaRepo, PageRepo pageRepo, SearchIndexRepo indexRepo, SiteRepo siteRepo) {
        this.lemmaRepo = lemmaRepo;
        this.pageRepo = pageRepo;
        this.indexRepo = indexRepo;
        this.siteRepo = siteRepo;
    }
}
