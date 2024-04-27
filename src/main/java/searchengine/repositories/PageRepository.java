package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    Optional<PageEntity> findByPath(String path);
    Optional<PageEntity> findByPathAndSite(String path, SiteEntity site);

    @Query("SELECT COUNT(p) FROM PageEntity p WHERE p.site = :site AND p.pageStatus = 'INDEXED'")
    long countIndexedPagesBySite(SiteEntity site);

}
