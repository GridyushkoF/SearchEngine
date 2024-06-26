package searchengine.util;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

@Log4j2
public class LemmasExtractorUtil {
    public LemmasExtractorUtil() {
        try {
            this.luceneMorphology = new RussianLuceneMorphology();
            this.validator = new LemmasValidatorUtil(luceneMorphology);
        } catch (Exception e) {
            log.error(e);
        }

    }

    private LemmasValidatorUtil validator;
    private LuceneMorphology luceneMorphology;

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
        List<String> split = new ArrayList<>();
        List.of(text.split(" ")).forEach(word -> {
            split.addAll(splitWordBySymbols(word));
        });
        return split;
    }

    public Set<String> getUniqueLemmasFromText(String text) {
        Set<String> lemmas = Collections.synchronizedSet(new HashSet<>());
        List<String> words = splitTextOnWords(text);
        words.parallelStream().forEach(word -> {
            String wordNormalForm = getWordNormalForm(word);
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
            String wordNormalForm = getWordNormalForm(word);
            lemmas.add(Objects.requireNonNullElse(wordNormalForm, word));
        });
        return lemmas;
    }

    public HashMap<String, Integer> getLemmas2RankingFromText(String text) {
        List<String> lemmaSet = getNotUniqueStringLemmasFromText(text);
        HashMap<String, Integer> lemmas2Ranking = new HashMap<>();
        lemmaSet.parallelStream().forEach(lemma -> lemmas2Ranking.put(
                lemma,
                lemmas2Ranking.getOrDefault(lemma, 0) + 1
        ));
        return lemmas2Ranking;
    }

    public String mergeLemmasToText(List<String> lemmaList) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        lemmaList.forEach(stringJoiner::add);
        return stringJoiner.toString();
    }

    public String getWordNormalForm(String word) {
        String wordNormalForm = word.toLowerCase();
        try {
            if (!wordNormalForm.matches(LemmasValidatorUtil.SYMBOLS_REGEX)) {
                wordNormalForm = wordNormalForm.replaceAll(LemmasValidatorUtil.SYMBOLS_REGEX, "");
            }
            wordNormalForm = convertToStandardFormIfWordIsRussian(wordNormalForm);
        } catch (Exception e) {
            log.error("NPE (null pointer exception): getNormalForms(word)).get(0) == null/word = " + wordNormalForm, e);
        }
        return wordNormalForm;
    }

    private String convertToStandardFormIfWordIsRussian(String word) {
        String normalizedWord = word;
        if (!normalizedWord.isEmpty() && validator.isCyrillic(normalizedWord)) {
            List<String> wordNormalForms = luceneMorphology.getNormalForms(normalizedWord);
            if (wordNormalForms != null) {
                normalizedWord = wordNormalForms.get(0);
            }
        }
        return normalizedWord;
    }

    public List<String> splitWordBySymbols(String word) {
        String wordWithSymbolsReplacedOnSpaces = word.replaceAll(LemmasValidatorUtil.SYMBOLS_REGEX, " ");
        return List.of(wordWithSymbolsReplacedOnSpaces.split(" "));
    }
}
