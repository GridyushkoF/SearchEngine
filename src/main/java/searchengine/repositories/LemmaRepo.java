package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepo extends CrudRepository<Lemma, Integer> {
    Optional<Lemma> findByLemma(String lemma);

    List<Lemma> findAllByLemma(String lemma);

    @Query("select l.lemma from Lemma l group by l.lemma,l.site having count(*) > 1")
    List<String> findAllDoubleLemmasStringList();

}
