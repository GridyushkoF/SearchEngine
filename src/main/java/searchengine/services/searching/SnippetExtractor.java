package searchengine.services.searching;

import searchengine.model.Page;
import searchengine.services.lemmas.LemmatizationService;

import java.util.*;

public class SnippetExtractor {
    public String extractHtmlSnippet(Page page, Set<String> lemmaList) {
        String pageContent = LemmatizationService.removeTags(page.getContent());
        List<String> contentWordList = List.of(pageContent.split(" "));
        List<String> contentWordListWithBoldLemmas = new ArrayList<>();
        for (String contentWord : contentWordList) {
            if(contentWord.isEmpty()) {continue;}
            String normalizedWord = contentWord.toLowerCase();
            if(LemmatizationService.isCyrillic(contentWord)) {
                List<String> wordForms = LemmatizationService.getNormalForms(contentWord);
                normalizedWord = wordForms != null && !wordForms.isEmpty() ? wordForms.get(0) : normalizedWord;
            }
            contentWordListWithBoldLemmas.add(lemmaList.contains(normalizedWord) ? (setStringBold(contentWord)) : contentWord);
        }
        contentWordListWithBoldLemmas = normalizeHtmlSnippet(contentWordListWithBoldLemmas);
        String snippet;
        int longestBoldSnippetIndex = 0;
        int maxSnippetLength = 0;
        for (int i = 0; i < contentWordListWithBoldLemmas.size(); i++) {
            String snippetPhrase = contentWordListWithBoldLemmas.get(i);
            if (isBoldString(snippetPhrase) && snippetPhrase.length() > maxSnippetLength) {
                maxSnippetLength = snippetPhrase.length();
                longestBoldSnippetIndex = i;
            }
        }
        int startSublistIndex = Math.max(0, longestBoldSnippetIndex - 10);
        int endSubListIndex = Math.min(contentWordListWithBoldLemmas.size(), longestBoldSnippetIndex + 10);
        snippet = contentWordListWithBoldLemmas.subList(startSublistIndex, endSubListIndex)
                .toString().replaceAll("[\\[\\],]", "");
        return cutOffSnippetByFirstSpaceAfterLimit(snippet);
    }
    public List<String> normalizeHtmlSnippet(List<String> wordList) {
        List<String> normalizedWords = new ArrayList<>();
        int iterationSkipCount = 0;
        for (int i = 0; i < wordList.size(); i++) {
            if (iterationSkipCount > 0) {
                iterationSkipCount--;
                continue;
            }
            String currentWord = wordList.get(i);
            if (i + 2 >= wordList.size()) {
                String nextWord = wordList.get(i + 1);
                if(isBoldString(currentWord) && isBoldString(nextWord)) {
                    normalizedWords.add(mergeBoldStrings(currentWord,nextWord));
                }
                break;
            }
            String nextWord = wordList.get(i + 1);
            String nextNextWord = wordList.get(i + 2);
            if (isBoldString(currentWord) && isBoldString(nextNextWord)) {
                iterationSkipCount = 2;
                normalizedWords.add(mergeBoldStrings(currentWord,nextWord,nextNextWord));
            } else if (isBoldString(currentWord) && isBoldString(nextWord)) {
                normalizedWords.add(mergeBoldStrings(currentWord,nextWord));
                iterationSkipCount = 1;
            } else  {
                normalizedWords.add(currentWord);
            }
        }
        return normalizedWords;
    }
    public String cutOffSnippetByFirstSpaceAfterLimit(String htmlSnippet) {
        int limit = 120;
        if (htmlSnippet.length() >= limit) {
            StringBuilder newSnippet = new StringBuilder();
            boolean isSplicePlace = false;
            for (int i = 0; i < htmlSnippet.length(); i++) {
                char currentSymbol = htmlSnippet.charAt(i);
                if (i % limit == 0 && i > 0) {
                    isSplicePlace = true;
                }
                if (htmlSnippet.charAt(i) == ' ' && isSplicePlace) {
                    isSplicePlace = false;
                    newSnippet.append(currentSymbol).append("\n");
                } else {
                    newSnippet.append(currentSymbol);
                }

            }
            return newSnippet.toString();
        }
        return htmlSnippet;
    }
    public boolean isBoldString(String string) {
        return string.startsWith("<b>") && string.endsWith("</b>");
    }
    public String setStringBold(String string) {return "<b>" + string + "</b>";}
    public String mergeBoldStrings(String ... strings) {
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = 0; i < strings.length; i++) {
            String currentWord = strings[i];
            if(i + 1 >= strings.length) {
                currentWord = currentWord.replaceAll("<b>", "");
                joiner.add(currentWord);
                break;
            }
            String nextWord = strings[i + 1];
            currentWord = currentWord.replaceAll("</b>", "");
            nextWord = nextWord.replaceAll("<b>", "");
            joiner.add(currentWord).add(nextWord);
        }
        return joiner.toString();
    }
}
