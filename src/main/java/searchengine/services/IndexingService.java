package searchengine.services;

import org.springframework.data.domain.Page;
import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse startIndexing();
}
