package searchengine.services;

import searchengine.dto.indexPage.IndexingPageResponse;

public interface IndexingPageService {
    IndexingPageResponse startIndexingPage(String pagePath);
}
