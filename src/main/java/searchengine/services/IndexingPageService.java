package searchengine.services;

import searchengine.dto.indexing.IndexingPageResponse;
import searchengine.dto.indexing.IndexingResponse;

public interface IndexingPageService {
    IndexingPageResponse startIndexingPage(String pagePath);


}
