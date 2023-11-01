package searchengine.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma,Integer> {
    Optional<Lemma> findByLemma(String lemma);
}
