package searchengine.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;

import java.util.List;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {

    List<SearchIndex> findAllByPage(Page Page);

    List<SearchIndex> findAllByLemma(Lemma Lemma);
    @Query("select i from SearchIndex i where i.lemma.lemma = ?1")
    List<SearchIndex> findAllByLemmaString(String lemma);
}
