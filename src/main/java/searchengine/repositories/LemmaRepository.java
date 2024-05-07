package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    Optional<LemmaEntity> findByLemma(String lemma);
    List<LemmaEntity> findAllByLemma(String lemma);

    @Query("select l.lemma from LemmaEntity l group by l.lemma,l.site having count(*) > 1")
    List<String> findAllDoubleLemmasStringList();
    int countBySite(SiteEntity site);
}
