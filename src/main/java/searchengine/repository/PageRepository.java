package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<PageEntity, Integer> {
    PageEntity findByPagePathAndSiteEntity(String pagePath, SiteEntity siteEntity);
    PageEntity findByPagePath(String pagePath);
    List<PageEntity> findAllBySiteEntity(SiteEntity siteEntity);
    PageEntity findById(int pageId);

}
