package searchengine.dto.others;

import lombok.Getter;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;

import java.util.HashSet;
import java.util.Set;
@Getter
public class IndexingTempData {
    private final Set<LemmaEntity> tempLemmas;
    private final Set<IndexEntity> tempIndexes;
    public IndexingTempData() {
        this.tempLemmas = new HashSet<>();
        this.tempIndexes = new HashSet<>();
    }
}
