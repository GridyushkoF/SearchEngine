package searchengine.services.lemmas;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class LemmatizationService {
    private final LuceneMorphology luceneMorph = new RussianLuceneMorphology();

    public LemmatizationService() throws IOException {
    }

    public HashMap<String,Integer> getLemmas (String text) {
        HashMap<String,Integer> lemmas2Count = new HashMap<>();
        text = text.replaceAll("[^А-Яа-я ]","").replaceAll("\\s+", " ").toLowerCase();
        List<String> words = List.of(text.split(" "));
        words.forEach(word -> {
            if (notFunctional(word))
            {
                String wordNormalForm = luceneMorph.getNormalForms(word).get(0);
                lemmas2Count.put(wordNormalForm,lemmas2Count.getOrDefault(wordNormalForm,0) + 1);

            }
        });
        return lemmas2Count;
    }
    public boolean notFunctional (String word) {
        String mainInfo = luceneMorph.getMorphInfo(word.toLowerCase()).get(0);
        return !mainInfo.contains("СОЮЗ") && ! mainInfo.contains("МЕЖД") && ! mainInfo.contains("ПРЕДЛ");
    }
    public String removeTags(String html) {
        return Jsoup.clean(html, Safelist.none());
    }
}
