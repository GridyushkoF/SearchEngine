package searchengine.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

;

@Repository
public interface SearchIndexRepository extends CrudRepository<SearchIndex,Integer> {
    List<SearchIndex> findAllByPage(Page page);
}
