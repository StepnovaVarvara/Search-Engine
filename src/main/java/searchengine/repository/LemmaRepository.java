package searchengine.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends CrudRepository<LemmaEntity, Integer> {
    LemmaEntity findByLemmaName(String lemmaName);

    @Query("")//TODO Vzyat u Mishi
    List<LemmaEntity> findByLemmaNameAndSites(String lemmaName, List<Integer> siteIdList);
    void deleteById(int lemmaId);
    List<LemmaEntity> findAllBySite(SiteEntity site);
}
