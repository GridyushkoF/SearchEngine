package searchengine.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteRepository extends CrudRepository<Site, Long> {
    boolean existsByUrl (String url);
    Optional<Site> findByUrl (String url);
}
