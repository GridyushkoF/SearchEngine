package searchengine.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import searchengine.model.PageEntity;
import searchengine.model.SiteStatus;
import searchengine.services.indexing.IndexingService;

@RequiredArgsConstructor
@Log4j2
public class LemmasValidatorUtil {

    private final LuceneMorphology luceneMorphology;
    private static final char FIRST_RUSSIAN_ALPHABET_SYMBOL = 'А';
    private static final char LAST_RUSSIAN_ALPHABET_SYMBOL = 'я';
    public static final String SYMBOLS_REGEX = "[^a-zA-Z0-9а-яА-Я\\s]+";

    public boolean isCyrillic(String text) {
        for (int i = 0; i < text.length(); i++) {
            char symbol = text.charAt(i);
            if
            ((symbol < FIRST_RUSSIAN_ALPHABET_SYMBOL
                ||
                symbol > LAST_RUSSIAN_ALPHABET_SYMBOL)
                && symbol != 'ё'
                && symbol != 'Ё'
            ) {
                return false;
            }
        }
        return true;
    }
    public boolean isNotFunctional(String word) {
        try {
            if (isCyrillic(word)) {
                String mainInfo = luceneMorphology.getMorphInfo(word.toLowerCase()).get(0);
                return !mainInfo.contains("СОЮЗ") && !mainInfo.contains("МЕЖД") && !mainInfo.contains("ПРЕДЛ");
            }
        } catch (Exception e) {
            log.error(LogMarkersUtil.EXCEPTIONS,"Exception: can`t check word on 'not functional'", e);
        }
        return false;
    }
    public boolean shouldIndexPage(PageEntity page, boolean ignoreIndexingStatus) {
        return (page.getSite().getStatus().equals(SiteStatus.INDEXING))
                || ignoreIndexingStatus
                || IndexingService.isIndexing();
    }
    public boolean isPageIndexingNow(PageEntity page) {
        return (((page.getSite().getStatus().equals(SiteStatus.INDEXING))
                && IndexingService.isIndexing()));
    }
}
