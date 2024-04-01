package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.IndexProperties;
import searchengine.dto.indexing.IndexingRsDto;
import searchengine.exceptions.IndexingException;
import searchengine.variables.FJP;
import searchengine.variables.IndexProcessVariables;

@Service
@RequiredArgsConstructor
@Slf4j
public class StopIndexingServiceImpl implements StopIndexingService {

    private final IndexProperties indexProperties;

    @Override
    @SneakyThrows
    public IndexingRsDto stopIndexing() {
        log.info("stopIndexing > начал работу: {}", IndexProcessVariables.isRUNNING());
        if (IndexProcessVariables.isRUNNING()) {
            IndexProcessVariables.setRUNNING(false);

            Thread.sleep(1500);
            FJP.getInstance().shutdownNow();

            while (FJP.getInstance().getActiveThreadCount() > 0) {}
            log.info("Потоки FJP остановаились: {}", FJP.getInstance().getActiveThreadCount());

            Thread.sleep(1500);
            log.info("stopIndexing > завершился: {}", IndexProcessVariables.isRUNNING());
            return new IndexingRsDto().setResult(true);
        } else {
            throw new IndexingException(indexProperties.getMessages().getStopError());
        }
    }
}