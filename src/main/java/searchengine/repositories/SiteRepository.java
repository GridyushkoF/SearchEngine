package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends CrudRepository<Site, Long> {
    Optional<Site> findByUrl(String url);
    List<Site> findAllByUrl(String url);
}
