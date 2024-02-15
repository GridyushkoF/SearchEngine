package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    Optional<Page> findByPath(String path);

    @Query("SELECT COUNT(p) FROM Page p WHERE p.site = :site AND p.pageStatus = 'INDEXED'")
    long countIndexedPagesBySite(Site site);

}
