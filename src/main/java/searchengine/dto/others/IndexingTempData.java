package searchengine.dto.others;

import lombok.Getter;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;

import java.util.HashSet;
import java.util.Set;
@Getter
public class IndexingTempData {
    private Set<LemmaEntity> tempLemmas;
    private Set<IndexEntity> tempIndexes;

    public IndexingTempData(Set<LemmaEntity> lemmas, Set<IndexEntity> indexes) {
        this.tempLemmas = lemmas;
        this.tempIndexes = indexes;
    }
    public IndexingTempData() {
        this.tempLemmas = new HashSet<>();
        this.tempIndexes = new HashSet<>();
    }
    public void setLemmasAndIndexesNull() {
        tempLemmas = null;
        tempIndexes = null;
    }
    public boolean isLemmasAndIndexesPresent() {
        return !tempLemmas.isEmpty() && !tempIndexes.isEmpty() ;
    }

}
