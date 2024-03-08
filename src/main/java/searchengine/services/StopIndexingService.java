package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

import java.util.concurrent.Future;

public interface StopIndexingService {
    IndexingResponse stopIndexing();
}
