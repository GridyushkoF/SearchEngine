package searchengine.dto.others;

import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;

import java.util.Set;
public record IndexingTempData(Set<LemmaEntity> tempLemmas, Set<IndexEntity> tempIndexes) {
}
