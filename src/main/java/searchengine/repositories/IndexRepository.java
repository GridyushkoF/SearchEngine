package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    List<IndexEntity> findAllByPage(PageEntity page);;
    List<IndexEntity> findAllByLemmaEntity(LemmaEntity lemmaEntity);
    @Query("select i from IndexEntity i where i.lemmaEntity.lemma = ?1")
    List<IndexEntity> findAllByLemma(String lemma);

}
