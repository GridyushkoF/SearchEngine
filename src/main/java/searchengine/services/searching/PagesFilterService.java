package searchengine.services.searching;

import org.springframework.stereotype.Service;
import searchengine.model.PageEntity;
import searchengine.util.LemmasExtractorUtil;

import java.util.List;
import java.util.Set;

@Service
public class PagesFilterService {
    private final LemmasExtractorUtil lemmasExtractorUtil = new LemmasExtractorUtil();

    public List<PageEntity> selectPagesContainingAllLemmas(List<PageEntity> pagesThatContainsRarestLemmaOfQuery, Set<String> filteredLemmasSortedByFrequency) {
        return pagesThatContainsRarestLemmaOfQuery.stream().filter(page -> {
                    Set<String> lemmasInPage =
                            lemmasExtractorUtil.getUniqueLemmasFromText(lemmasExtractorUtil.removeHtmlTags(page.getContent()));
                    return lemmasInPage.containsAll(filteredLemmasSortedByFrequency);
                })
                .toList();
    }
}
