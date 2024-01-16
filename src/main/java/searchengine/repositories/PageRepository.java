package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    Optional<Page> findByPath(String path);

    long countPageBySite(Site Site);

}
