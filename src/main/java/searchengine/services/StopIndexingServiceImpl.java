package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.IndexProperties;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.IndexingException;
import searchengine.variables.IndexProcessVariables;

@Service
@RequiredArgsConstructor
public class StopIndexingServiceImpl implements StopIndexingService {

    private final IndexProperties indexProperties;

    @Override
    public IndexingResponse stopIndexing() {
        if (IndexProcessVariables.isRUNNING()) {
            IndexProcessVariables.setRUNNING(false);
            return new IndexingResponse().setResult(true);
        } else {
            throw new IndexingException(indexProperties.getMessages().getStopError());
        }
    }
}