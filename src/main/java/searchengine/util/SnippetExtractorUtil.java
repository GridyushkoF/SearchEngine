package searchengine.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.PageEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SnippetExtractorUtil {
    private final LemmasExtractorUtil extractor = new LemmasExtractorUtil();
    public static final int SNIPPET_LENGTH_LIMIT = 400;
    public String getHtmlSnippet(PageEntity page, Set<String> lemmaList) {
        lemmaList = lemmaList.stream()
                .map(extractor::getWordNormalForm)
                .collect(Collectors.toSet());
        String pageContentWithoutTags = extractor.removeHtmlTags(page.getContent());

        List<String> contentWordList = List.of(pageContentWithoutTags.split(" "));
        List<String> contentWordListWithBoldLemmas =
                getContentWordListWithBoldLemmas(lemmaList, contentWordList);
        List<Integer> longestBoldLemmasRow = findLongestBoldLemmasRow(contentWordListWithBoldLemmas);
        String snippet = getSnippetByLongestBoldRow(contentWordListWithBoldLemmas, longestBoldLemmasRow);
        return cutOffSnippet(snippet);
    }
    //will changed
    private String getSnippetByLongestBoldRow(List<String> contentWordListWithBoldLemmas, List<Integer> longestBoldLemmasRow) {
        String snippet;
        int startSublistIndex = Math.max(0, longestBoldLemmasRow.get(0) - 12);
        int endSubListIndex = Math.min(contentWordListWithBoldLemmas.size(),
                longestBoldLemmasRow.get(longestBoldLemmasRow.size() - 1) + 12);

        snippet = extractor.mergeLemmasToText(
                    contentWordListWithBoldLemmas.subList(
                        startSublistIndex,
                        endSubListIndex)
                );
        return snippet;
    }
    //should not change
    public List<String> getContentWordListWithBoldLemmas(Set<String> lemmas, List<String> contentWords) {
        List<String> contentWordListWithBoldLemmas = new ArrayList<>();
        for (String contentWord : contentWords) {
            if(contentWord.isEmpty()) {
                continue;
            }
            if (isWordContainsSymbolsAndLemma(lemmas, contentWord)) {
                contentWordListWithBoldLemmas.add(setStringBold(contentWord));
                continue;
            }
            String normalizedWord = extractor.getWordNormalForm(contentWord);
            contentWordListWithBoldLemmas.add(lemmas.contains(normalizedWord) ? (setStringBold(contentWord)) : contentWord);
        }
        return contentWordListWithBoldLemmas;
    }
    //should not change
    private boolean isWordContainsSymbolsAndLemma(Set<String> lemmas, String contentWord) {
        Pattern pattern = Pattern.compile(LemmasValidatorUtil.SYMBOLS_REGEX);
        Matcher matcher = pattern.matcher(contentWord);
        if(matcher.find()) {
            for (String lemma : lemmas) {
                if(contentWord.contains(lemma)) {
                    return true;
                }
            }
        }
        return false;
    }
    //need change
    public List<Integer> findLongestBoldLemmasRow(List<String> contentWordListWithBoldLemmas) {
        List<Integer> longestBoldLemmasRow = new ArrayList<>();
        List<Integer> currentBoldLemmasRow = new ArrayList<>();

        for (int i = 0; i < contentWordListWithBoldLemmas.size(); i++) {
            if (isBoldString(contentWordListWithBoldLemmas.get(i))) {
                currentBoldLemmasRow.add(i);
            } else if (!currentBoldLemmasRow.isEmpty()) {
                if (currentBoldLemmasRow.size() > longestBoldLemmasRow.size()) {
                    longestBoldLemmasRow = new ArrayList<>(currentBoldLemmasRow);
                }
                currentBoldLemmasRow.clear();
            }
        }
        if (!currentBoldLemmasRow.isEmpty() && currentBoldLemmasRow.size() > longestBoldLemmasRow.size()) {
            longestBoldLemmasRow = new ArrayList<>(currentBoldLemmasRow);
        }

        return longestBoldLemmasRow.isEmpty() ? List.of(0) : longestBoldLemmasRow;
    }


    public String cutOffSnippet(String snippet) {
        return snippet.substring(0,Math.min(SNIPPET_LENGTH_LIMIT,snippet.length())) + "...";
    }
    public boolean isBoldString(String string) {
        return string.startsWith("<b>") && string.endsWith("</b>");
    }
    public String setStringBold(String string) {return "<b>" + string + "</b>";}
}
