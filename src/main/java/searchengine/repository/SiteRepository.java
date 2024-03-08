package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<SiteEntity, Integer> {
    SiteEntity findBySiteUrl(String siteUrl);
    List<SiteEntity> findAll();
}
