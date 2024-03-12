package searchengine.services.lemmas;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

@Log4j2
@RequiredArgsConstructor
public class LemmaExtractor {

    private final LemmaValidator validator;
    private final LuceneMorphology morphology;

    public String removeHtmlTagsAndNormalize(String html) {
        return normalizeText(removeHtmlTags(html));
    }
    public String removeHtmlTags(String html) {
        Document document = Jsoup.parse(html);
        return document.text();
    }
    public String normalizeText(String lemma) {
        return lemma.replaceAll("[^а-яА-ЯA-Za-z.,;:/\\-0-9Её&@ ]", "")
                .replaceAll("\\s+", " ").toLowerCase();
    }
    public List<String> getWordsFromText(String text) {
        text = removeHtmlTagsAndNormalize(text);
        return List.of(text.split(" "));
    }
    public String getWordNormalForm(String word) {
        String wordNormalForm = word.toLowerCase();
        String symbolsRegex = LemmaValidator.SYMBOLS_REGEX;
        try {
            if(!word.matches(symbolsRegex)) {
                wordNormalForm = wordNormalForm.replaceAll(symbolsRegex,"");
            }

            if(validator.isCyrillic(wordNormalForm)) {
                wordNormalForm = Objects.requireNonNull(morphology.getNormalForms(wordNormalForm)).get(0);
            }
        } catch (NullPointerException e) {
            log.error("NPE (null pointer exception): getNormalForms(word)).get(0) == null", e);
        }
        return wordNormalForm;
    }
    public Set<String> getLemmasSetFromText(String text) {
        Set<String> lemmas = new HashSet<>();
        List<String> words = getWordsFromText(text);
        words.forEach(word -> {
            String wordNormalForm = getWordNormalForm(word);
            if (wordNormalForm != null) {
                lemmas.add(wordNormalForm);
            } else {
                lemmas.add(word);
            }

        });
        return lemmas;
    }

    public HashMap<String, Integer> getLemmas2RankingFromText(String text) {
        Set<String> lemmaSet = getLemmasSetFromText(text);
        HashMap<String, Integer> lemmas2Ranking = new HashMap<>();
        lemmaSet.forEach(lemma -> {
            lemmas2Ranking.put
                    (
                            lemma,
                            lemmas2Ranking.getOrDefault(lemma, 0) + 1
                    );
        });
        return lemmas2Ranking;
    }
    public String getTextOfLemmaList(List<String> lemmaList) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        lemmaList.forEach(stringJoiner::add);
        return  stringJoiner.toString();
    }
}
