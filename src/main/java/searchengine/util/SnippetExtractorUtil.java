package searchengine.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.PageEntity;
import searchengine.services.lemmas.LemmaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SnippetExtractorUtil {
    private final LemmaService lemmaService;
    private static final int SNIPPET_LENGTH_LIMIT = 300;
    public String getHtmlSnippet(PageEntity page, Set<String> lemmaList) {
        lemmaList = lemmaList.stream()
                .map(lemmaService.getExtractor()::getWordNormalForm)
                .collect(Collectors.toSet());
        String pageContentWithoutTags = lemmaService
                .getExtractor()
                .removeHtmlTags(page.getContent());

        List<String> contentWordList = List.of(pageContentWithoutTags.split(" "));
        List<String> contentWordListWithBoldLemmas =
                getContentWordListWithBoldLemmas(lemmaList, contentWordList);
        List<Integer> longestBoldLemmasRow = findLongestBoldLemmasRow(contentWordListWithBoldLemmas);
        String snippet = getSnippetByLongestBoldRow(contentWordListWithBoldLemmas, longestBoldLemmasRow);
        return cutOffSnippet(snippet);
    }

    private String getSnippetByLongestBoldRow(List<String> contentWordListWithBoldLemmas, List<Integer> longestBoldLemmasRow) {
        String snippet;
        int startSublistIndex = Math.max(0, longestBoldLemmasRow.get(0) - 12);
        int endSubListIndex = Math.min(contentWordListWithBoldLemmas.size(),
                longestBoldLemmasRow.get(longestBoldLemmasRow.size() - 1) + 12);

        snippet = lemmaService
                .getExtractor()
                .mergeLemmasToText(
                    contentWordListWithBoldLemmas.subList(
                        startSublistIndex,
                        endSubListIndex)
                );
        return snippet;
    }

    private List<String> getContentWordListWithBoldLemmas(Set<String> lemmaList, List<String> contentWordList) {
        List<String> contentWordListWithBoldLemmas = new ArrayList<>();
        for (String contentWord : contentWordList) {
            if(contentWord.isEmpty()) {
                continue;
            }

            String normalizedWord = lemmaService
                    .getExtractor().getWordNormalForm(contentWord);
            contentWordListWithBoldLemmas.add(lemmaList.contains(normalizedWord) ? (setStringBold(contentWord)) : contentWord);
        }
        return contentWordListWithBoldLemmas;
    }

    public List<Integer> findLongestBoldLemmasRow(List<String> contentWordListWithBoldLemmas) {
        List<Integer> longestBoldLemmasRow = new ArrayList<>();
        List<Integer> currentBoldLemmasRow = new ArrayList<>();
        int longestLemmasCount = 0;
        int currentLemmasCount = 0;

        for (int i = 0; i < contentWordListWithBoldLemmas.size(); i++) {
            String currentWord = contentWordListWithBoldLemmas.get(i);
            boolean isLastWord = i == contentWordListWithBoldLemmas.size() - 1;

            currentLemmasCount = getCurrentLemmasCount(contentWordListWithBoldLemmas, currentBoldLemmasRow, currentLemmasCount, i, currentWord);

            if ((!isBoldString(currentWord) || isLastWord) && isLastWord && isBoldString(currentWord) && contentWordListWithBoldLemmas.contains(currentWord)) {
                currentLemmasCount++;
            }

            longestLemmasCount = getLongestLemmasCount(longestBoldLemmasRow, currentBoldLemmasRow, longestLemmasCount, currentLemmasCount);
            currentBoldLemmasRow.clear();
            currentLemmasCount = 0;
        }

        if (longestBoldLemmasRow.isEmpty()) {
            return List.of(0);
        }
        return longestBoldLemmasRow;
    }

    private int getLongestLemmasCount(List<Integer> longestBoldLemmasRow, List<Integer> currentBoldLemmasRow, int longestLemmasCount, int currentLemmasCount) {
        if (longestBoldLemmasRow.size() < currentBoldLemmasRow.size() ||
                (longestBoldLemmasRow.size() == currentBoldLemmasRow.size()
                        && longestLemmasCount < currentLemmasCount)) {
            longestBoldLemmasRow.clear();
            longestBoldLemmasRow.addAll(currentBoldLemmasRow);
            longestLemmasCount = currentLemmasCount;
        }
        return longestLemmasCount;
    }

    private int getCurrentLemmasCount(List<String> contentWordListWithBoldLemmas, List<Integer> currentBoldLemmasRow, int currentLemmasCount, int i, String currentWord) {
        if (isBoldString(currentWord)) {
            currentBoldLemmasRow.add(i);
            if (contentWordListWithBoldLemmas.contains(currentWord)) {
                currentLemmasCount++;
            }
        }
        return currentLemmasCount;
    }

    public String cutOffSnippet(String snippet) {
        return snippet.substring(0,Math.min(SNIPPET_LENGTH_LIMIT,snippet.length())) + "...";
    }
    public boolean isBoldString(String string) {
        return string.startsWith("<b>") && string.endsWith("</b>");
    }
    public String setStringBold(String string) {return "<b>" + string + "</b>";}
}
