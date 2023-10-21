package searchengine.services.indexing;
import lombok.Getter;
import lombok.extern.log4j.Log4j;
import org.jsoup.Connection;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;
import searchengine.services.lemmas.LemmatizationService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j
@Getter
public class SiteWalker extends RecursiveAction {
    //field and constructor:
    public SiteWalker(NodeLink curNodeLink, Site rootSite, PageRepository pageRepository, SiteRepository siteRepository, LemmaRepository lemmaRepository) {
        this.curNodeLink = curNodeLink;
        this.rootSite = rootSite;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
    }
    private final NodeLink curNodeLink;
    private final Site rootSite;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private static final Set<String> VISITED_LINKS = new HashSet<>();
    private static final LemmatizationService service;

    static {
        try {
            service = new LemmatizationService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //alg.code:
    @Override
    protected void compute() {
        getTasks();
    }
    @Transactional
    public void getTasks() {

                curNodeLink.getChildren().forEach(child -> {
                    try {
                        String absoluteLink = child.getLink();
                        String pathLink = getPathOf(absoluteLink);
                        if (notVisited(absoluteLink) && !pathLink.isEmpty() && IndexingService.isIndexing()) {
                            System.out.println("Состояние сервиса индексации: " + IndexingService.isIndexing());
                            new SiteWalker(child, rootSite, pageRepository, siteRepository,lemmaRepository).fork();
                            Connection.Response response = curNodeLink.getResponse();
                            Page curPage = new Page(
                                    rootSite,
                                    pathLink,
                                    response.statusCode(),
                                    response.parse().toString()
                            );
                            pageRepository.save(curPage);
                            setCurrentTimeToRootSite();
                            System.out.println("\u001B[32m" + "Добавлена новая страница с путём: " + pathLink + "\u001B[0m" + " от сайта " + rootSite.getId());
                            String fixedHTML = service.removeTags(curPage.getContent());
                            HashMap<String,Integer> lemmas2count = service.getLemmas(fixedHTML);
                            for (Map.Entry<String,Integer> entry : lemmas2count.entrySet()) {
                                String lemmaText = entry.getKey();
                                Optional<Lemma> lemmaOptional = lemmaRepository.findByLemma(lemmaText);
                                if(lemmaOptional.isPresent()) {
                                    Lemma lemma = lemmaOptional.get();
                                    lemma.setFrequency(lemma.getFrequency() + 1);
                                    lemmaRepository.save(lemma);
                                } else {
                                    Integer count = entry.getValue();
                                    lemmaRepository.save(new Lemma(rootSite,lemmaText,count));
                                }
                            }

                        }

                    } catch (Exception e) {
                        System.out.println("ОШИБКА ОТ GETTASKS(): + trace:");
//                        e.printStackTrace();
                    }
                });
    }
    public String getPathOf(String link) throws URISyntaxException {return new URI(link).getPath();}
    private boolean notVisited(String link)
    {
        if (!VISITED_LINKS.contains(link))
        {
            VISITED_LINKS.add(link);
            return true;
        }
        return false;
    }
    private void setCurrentTimeToRootSite ()
    {
        rootSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(rootSite);
    }
}