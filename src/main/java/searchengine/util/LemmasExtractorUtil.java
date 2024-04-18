package searchengine.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

@Log4j2
@RequiredArgsConstructor
@Getter
public class LemmasExtractorUtil {

    public final LemmaExtractorCacheProxy lemmaExtractorCacheProxy;


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

    public List<String> splitTextOnWords(String text) {
        text = removeHtmlTagsAndNormalize(text);
        return List.of(text.split(" "));
    }

    public Set<String> getUniqueStringLemmasFromText(String text) {
        Set<String> lemmas = Collections.synchronizedSet(new HashSet<>());
        List<String> words = splitTextOnWords(text);
        words.parallelStream().forEach(word -> {
            String wordNormalForm = lemmaExtractorCacheProxy.getWordNormalForm(word);
            if (wordNormalForm != null) {
                lemmas.add(wordNormalForm);
            }
        });
        return lemmas;
    }

    public List<String> getNotUniqueStringLemmasFromText(String text) {
        List<String> lemmas = new ArrayList<>();
        List<String> words = splitTextOnWords(text);
        words.parallelStream().forEach(word -> {
            String wordNormalForm = lemmaExtractorCacheProxy.getWordNormalForm(word);
            lemmas.add(Objects.requireNonNullElse(wordNormalForm, word));
        });
        return lemmas;
    }

    public HashMap<String, Integer> getLemmas2RankingFromText(String text) {
        List<String> lemmaSet = getNotUniqueStringLemmasFromText(text);
        HashMap<String, Integer> lemmas2Ranking = new HashMap<>();
        lemmaSet.parallelStream().forEach(lemma -> {
            lemmas2Ranking.put(
                    lemma,
                    lemmas2Ranking.getOrDefault(lemma, 0) + 1
            );
        });
        return lemmas2Ranking;
    }

    public String mergeLemmasToText(List<String> lemmaList) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        lemmaList.forEach(stringJoiner::add);
        return stringJoiner.toString();
    }

}
