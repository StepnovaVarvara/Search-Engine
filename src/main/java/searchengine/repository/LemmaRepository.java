package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

@Repository
public interface LemmaRepository extends CrudRepository<LemmaEntity, Integer> {
    LemmaRepository findByLemmaName(String lemmaName);
}
