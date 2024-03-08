package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionSettings;
import searchengine.config.LemmaFinderSettings;
import searchengine.config.Site;
import searchengine.config.IndexProperties;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.IndexingException;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.variables.IndexProcessVariables;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final IndexProperties indexProperties;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final ConnectionSettings connectionSettings;
    private final LemmaFinderSettings lemmaFinderSettings;
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(6);

    @Override
    public IndexingResponse startIndexing() {
        if (IndexProcessVariables.isRUNNING()) {
            indexingSites();
            throw new IndexingException(indexProperties.getMessages().getStartError());
        } else {
            IndexingResponse indexingResponse = new IndexingResponse();
            indexingResponse.setResult(true);

            indexingSites();

            return indexingResponse;
        }
    }

    @SneakyThrows
    public void indexingSites() {
        IndexProcessVariables.setRUNNING(true);
        Date start = new Date();

        pageRepository.deleteAll();
        siteRepository.deleteAll();

        List<Site> siteList = indexProperties.getSites();
        for (Site site : siteList) {
            if (IndexProcessVariables.isRUNNING()) {
                PageRecursiveTask pageRecursiveTask = new PageRecursiveTask(site, connectionSettings, siteRepository,
                        indexProperties, pageRepository, true, lemmaRepository, indexRepository, lemmaFinderSettings);
                forkJoinPool.invoke(pageRecursiveTask);
            } else {
                saveSiteToDB(site, StatusType.FAILED, indexProperties.getMessages().getStop());
            }
        }

        Date finish = new Date();
        log.info("Index finish with time: {} ms", (finish.getTime() - start.getTime()));

        IndexProcessVariables.setRUNNING(false);
    }

    private void saveSiteToDB(Site site, StatusType statusType, String textOfLastError) {
        siteRepository.save(new SiteEntity()
                .setSiteName(site.getName())
                .setSiteUrl(site.getUrl())
                .setStatusIndexing(statusType)
                .setTextOfLastError(textOfLastError)
                .setStatusTime(LocalDateTime.now()));
    }
}