package searchengine.services.lemmas;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.services.indexing.IndexingService;

import java.util.*;

@Service
@Log4j2
public class LemmatizationService {
    private static LuceneMorphology RUSSIAN_MORPHOLOGY = null;

    static {
        try {
            RUSSIAN_MORPHOLOGY = new RussianLuceneMorphology();
        } catch (Exception e) {
            log.error("can`t init LuceneMorphology", e);
        }
    }

    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepo;

    @Autowired
    public LemmatizationService(LemmaRepository lemmaRepository, SearchIndexRepository indexRepo) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepo = indexRepo;
    }

    public static boolean notFunctional(String word) {
        try {
            if (isCyrillic(word)) {
                String mainInfo = RUSSIAN_MORPHOLOGY.getMorphInfo(word.toLowerCase()).get(0);
                return !mainInfo.contains("СОЮЗ") && !mainInfo.contains("МЕЖД") && !mainInfo.contains("ПРЕДЛ");
            }
        } catch (Exception e) {
            log.error("Exception: can`t check word on 'not functional'", e);
        }
        return false;
    }

    public static String removeTagsAndNormalize(String html) {
        return normalizeText(Jsoup.clean(html, Safelist.none()));
    }

    public static String removeTagsWithoutNormalization(String html) {
        return Jsoup.clean(html, Safelist.none());
    }

    public static List<String> getNormalForms(String word) {
        if (isCyrillic(word)) {
            return RUSSIAN_MORPHOLOGY.getNormalForms(word.toLowerCase());
        }
        return null;

    }

    public static boolean isCyrillic(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x0400 || c > 0x04FF) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeText(String lemma) {
        return lemma.replaceAll("[^а-яА-ЯA-Za-z ]", "").replaceAll("\\s+", " ").toLowerCase();
    }

    public HashMap<String, Integer> getLemmas2Ranking(String text) {
        HashMap<String, Integer> lemmas2Count = new HashMap<>();
        text = removeTagsAndNormalize(text);
        List<String> words = List.of(text.split(" "));
        words.forEach(word -> {
            if (isCyrillic(word)) {
                if (notFunctional(word)) {
                    String wordNormalForm = null;
                    try {
                        wordNormalForm = Objects.requireNonNull(getNormalForms(word)).get(0);
                    } catch (NullPointerException e) {
                        log.error("NPE (null pointer exception): getNormalForms(word)).get(0) == null", e);
                    }
                    if (wordNormalForm != null) {
                        lemmas2Count.put(wordNormalForm, lemmas2Count.getOrDefault(wordNormalForm, 0) + 1);
                    }
                }
            } else {
                lemmas2Count.put(word, lemmas2Count.getOrDefault(word, 0) + 1);
            }
        });
        return lemmas2Count;
    }

    public Set<String> getLemmasSet(String text) {
        Set<String> lemmas = new HashSet<>();
        text = removeTagsAndNormalize(text);
        List<String> words = List.of(text.split(" "));
        words.forEach(word -> {
            if (isCyrillic(word)) {
                if (notFunctional(word)) {
                    String wordNormalForm = RUSSIAN_MORPHOLOGY.getNormalForms(word).get(0);
                    if (wordNormalForm != null) {
                        lemmas.add(wordNormalForm);
                    }
                }
            } else {
                lemmas.add(word);
            }
        });
        return lemmas;
    }

    @Transactional
    public void getAndSaveLemmasAndIndexes(Page page) {
        Set<Lemma> localTempLemmas = new HashSet<>();
        Set<SearchIndex> localTempIndexes = new HashSet<>();
        var HTMLWithoutTags = removeTagsAndNormalize(page.getContent());
        var lemmas2count = getLemmas2Ranking(HTMLWithoutTags);
        for (var lemmaKey : lemmas2count.keySet()) {
            if (IndexingService.isIndexing()) {
                var lemmaList = lemmaRepository.findAllByLemma(lemmaKey);
                Lemma lemma;
                if (lemmaList.size() > 0) {
                    lemma = lemmaList.get(0);
                    lemma.setFrequency(lemma.getFrequency() + 1);
                } else {
                    lemma = new Lemma(page.getSite(), lemmaKey, 1);
                }
                localTempLemmas.add(lemma);
                localTempIndexes.add(new SearchIndex(page, lemma, lemmas2count.get(lemmaKey)));
            } else {
                break;
            }
        }
        lemmaRepository.saveAll(localTempLemmas);
        indexRepo.saveAll(localTempIndexes);
        log.info(page.getPath() + " all lemmas and indexes saved for this page\n\t\t" + localTempLemmas.stream().map(Lemma::getLemma).toList());
    }
}
