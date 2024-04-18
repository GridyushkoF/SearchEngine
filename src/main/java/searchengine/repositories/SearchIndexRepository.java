package searchengine.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;

import java.util.List;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {

    List<SearchIndex> findAllByPage(Page page);
    List<SearchIndex> findAllByLemma(Lemma lemma, Pageable pageable);
    List<SearchIndex> findAllByLemma(Lemma lemma);
    @Query("SELECT COUNT(si) FROM SearchIndex si WHERE si.lemma IN :lemmas")
    long countDistinctPagesByLemmas(@Param("lemmas") List<Lemma> lemmas);
    @Query("select i from SearchIndex i where i.lemma.lemma = ?1")
    List<SearchIndex> findAllByLemmaString(String lemma);

}
