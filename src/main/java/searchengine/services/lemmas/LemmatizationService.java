package searchengine.services.lemmas;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import searchengine.model.*;
import searchengine.services.RepoService;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class LemmatizationService {
    private static final HashSet<String> VISITED_LEMMAS = new HashSet<>();
    private final LuceneMorphology luceneMorph = new RussianLuceneMorphology();
    private final RepoService repoService;
    public LemmatizationService(RepoService repoService) throws IOException {
        this.repoService = repoService;
    }

    public HashMap<String,Integer> getLemmas(String text) {

        HashMap<String,Integer> lemmas2Count = new HashMap<>();
        text = text.replaceAll("[^А-Яа-я ]","").replaceAll("\\s+", " ").toLowerCase();
        List<String> words = List.of(text.split(" "));
        words.forEach(word -> {
            if (notFunctional(word))
            {
                String wordNormalForm = luceneMorph.getNormalForms(word).get(0);
                if (wordNormalForm != null) {
                    lemmas2Count.put(wordNormalForm,lemmas2Count.getOrDefault(wordNormalForm,0) + 1);
                }

            }
        });
        return lemmas2Count;
    }
    public boolean notFunctional(String word) {
        try {
            String mainInfo = luceneMorph.getMorphInfo(word.toLowerCase()).get(0);
            return !mainInfo.contains("СОЮЗ") && ! mainInfo.contains("МЕЖД") && ! mainInfo.contains("ПРЕДЛ");
        }catch (Exception e) {
            System.out.println("Маленькая ошибка морфологии");
        }
        return false;
    }
    public void addToIndex(Page page) {
        LemmaRepository lemmaRepo = repoService.getLemmaRepo();
        SearchIndexRepository indexRepo = repoService.getIndexRepo();
        new Thread(() -> {
            String HTMLWithoutTags = removeTags(page.getContent());
            HashMap<String,Integer> lemmas2count = getLemmas(HTMLWithoutTags);
            for (String key : lemmas2count.keySet()) {
                synchronized (lemmaRepo) {
                    synchronized (indexRepo) {
                        if(VISITED_LEMMAS.contains(key)) {
                            Optional<Lemma> lemmaOPT = lemmaRepo.findByLemma(key);
                            Lemma lemma;
                            if (lemmaOPT.isPresent()) {
                                lemma = lemmaOPT.get();
                                lemma.setFrequency(lemma.getFrequency() + 1);
                            } else {
                                lemma = new Lemma(page.getSite(), key, 1);
                            }
                            lemmaRepo.save(lemma);
                            indexRepo.save(new SearchIndex(page,lemma,lemmas2count.get(key)));
                        }
                    }
                }
                VISITED_LEMMAS.add(key);
            }
        }).start();
    }
    public static String removeTags(String html) {
        return Jsoup.clean(html, Safelist.none());
    }
}
