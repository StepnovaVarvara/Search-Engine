package searchengine.services;

import searchengine.dto.indexPage.IndexingPageRsDto;

public interface IndexingPageService {
    IndexingPageRsDto startIndexingPage(String pagePath);
}
