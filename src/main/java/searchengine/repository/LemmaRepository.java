package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends CrudRepository<LemmaEntity, Integer> {
    List<LemmaEntity> findAll();
    LemmaEntity findAllByLemmaNameAndSiteId(String lemmaName, int siteId);
    List<LemmaEntity> findAllByLemmaNameAndSiteIn(String lemmaName, List<SiteEntity> siteList);
    void deleteById(int lemmaId);
    List<LemmaEntity> findAllBySite(SiteEntity site);
}
