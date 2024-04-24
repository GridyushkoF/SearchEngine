package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    Optional<Page> findByPath(String path);
    Optional<Page> findByPathAndSite(String path, Site site);

    @Query("SELECT COUNT(p) FROM Page p WHERE p.site = :site AND p.pageStatus = 'INDEXED'")
    long countIndexedPagesBySite(Site site);

}
