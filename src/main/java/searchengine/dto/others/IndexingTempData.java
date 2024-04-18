package searchengine.dto.others;

import searchengine.model.Lemma;
import searchengine.model.SearchIndex;

import java.util.Set;
public record IndexingTempData(Set<Lemma> tempLemmas, Set<SearchIndex> tempIndexes) {
}
