package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<IndexEntity, Integer> {
    void deleteAllByPage(PageEntity pageEntity);
    List<IndexEntity> findAllByPage(PageEntity pageEntity);
    List<IndexEntity> findAllByLemma(LemmaEntity lemmaEntity);
    List<IndexEntity> findAllByPageAndLemma(PageEntity pageEntity, LemmaEntity lemmaEntity);
    IndexEntity findByPageAndLemma(PageEntity pageEntity, LemmaEntity lemmaEntity);
}
