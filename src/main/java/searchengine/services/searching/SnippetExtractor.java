package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.services.lemmas.LemmaValidator;
import searchengine.services.lemmas.LemmaService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SnippetExtractor {
    private final LemmaService lemmaService;
    public String getHtmlSnippet(Page page, Set<String> lemmaList) {
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
                .getTextOfLemmaList(
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
                    .getExtractor()
                    .getWordNormalForm(contentWord);
            contentWordListWithBoldLemmas.add(lemmaList.contains(normalizedWord) ? (setStringBold(contentWord)) : contentWord);
        }
        return contentWordListWithBoldLemmas;
    }

    public List<Integer> findLongestBoldLemmasRow(List<String> contentWordListWithBoldLemmas) {
        List<Integer> longestBoldLemmasRow = new ArrayList<>();
        List<Integer> currentBoldLemmasRow = new ArrayList<>();

        for (int i = 0; i < contentWordListWithBoldLemmas.size(); i++) {
            if (i == 0) {
                continue;
            }

            String prevWord = contentWordListWithBoldLemmas.get(i - 1);

            if (isBoldString(prevWord)) {
                currentBoldLemmasRow.add(i - 1);
            } else if((!isBoldString(prevWord))
                    || i == contentWordListWithBoldLemmas.size() - 1) {
                if (longestBoldLemmasRow.size() < currentBoldLemmasRow.size()) {
                    longestBoldLemmasRow.clear();
                    longestBoldLemmasRow.addAll(currentBoldLemmasRow);
                }
                if(lemmaService
                        .getValidator()
                        .isNotFunctional(prevWord)
                && !prevWord.matches(LemmaValidator.SYMBOLS_REGEX)) {
                    currentBoldLemmasRow.clear();
                }

            }
        }
        if(longestBoldLemmasRow.isEmpty()) {
            return List.of(0);
        }
        return longestBoldLemmasRow;
    }
    public String cutOffSnippet(String snippet) {
        int limit = 300;
        return snippet.substring(0,Math.min(limit,snippet.length())) + "...";
    }
    public boolean isBoldString(String string) {
        return string.startsWith("<b>") && string.endsWith("</b>");
    }
    public String setStringBold(String string) {return "<b>" + string + "</b>";}
}
