package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.indexing.IndexingRsDto;
import searchengine.exceptions.IndexingException;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.variables.FJP;
import searchengine.variables.IndexProcessVariables;

import java.time.LocalDateTime;
import java.util.List;

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
    private final PageRecursiveTaskProperties pageRecursiveTaskProperties;

    @SneakyThrows
    @Override
    public IndexingRsDto startIndexing() {
        if (FJP.getInstance().getActiveThreadCount() > 0) {
            IndexProcessVariables.setRUNNING(false);

            Thread.sleep(1500);
            FJP.getInstance().shutdownNow();

            while (FJP.getInstance().getActiveThreadCount() > 0) {}
            log.info("Потоки FJP остановаились: {}", FJP.getInstance().getActiveThreadCount());

            Thread.sleep(1500);
            indexingSites();
            log.info("startIndexing > повторно запустился");
            throw new IndexingException(indexProperties.getMessages().getStartError());
        } else {
            IndexingRsDto indexingRsDto = new IndexingRsDto();
            indexingRsDto.setResult(true);
            log.info("startIndexing > начал работу: {}", IndexProcessVariables.isRUNNING());

            indexingSites();

            log.info("startIndexing > завершился: {}", IndexProcessVariables.isRUNNING());
            return indexingRsDto;
        }
    }

    @SneakyThrows
    public void indexingSites() {
        IndexProcessVariables.setRUNNING(true);
        log.info("indexingSites > начал работу: {}", IndexProcessVariables.isRUNNING());

        siteRepository.deleteAll();
        pageRepository.deleteAll();

        List<Site> siteList = indexProperties.getSites();
        for (Site site : siteList) {
            if (IndexProcessVariables.isRUNNING()) {
                PageRecursiveTask pageRecursiveTask = new PageRecursiveTask(site, connectionSettings,
                        siteRepository, indexProperties, pageRepository, true,
                        lemmaRepository, indexRepository, lemmaFinderSettings, pageRecursiveTaskProperties);
                FJP.getInstance().invoke(pageRecursiveTask);
            } else {
                saveSiteToDB(site, StatusType.FAILED, indexProperties.getMessages().getStop());
            }
        }

        IndexProcessVariables.setRUNNING(false);
        log.info("indexingSites > завершился: {}", IndexProcessVariables.isRUNNING());
    }

    private void saveSiteToDB(Site site, StatusType statusType, String textOfLastError) {
        siteRepository.save(new SiteEntity()
                .setSiteName(site.getName())
                .setSiteUrl(site.getUrl())
                .setStatusIndexing(statusType)
                .setTextOfLastError(textOfLastError)
                .setStatusTime(LocalDateTime.now()));
    }
//    private String getSiteUrl(Site site) {
//        String siteUrl = null;
//        String regex = ".*\\bwww.\\b.*";
//        Pattern pattern = Pattern.compile(regex);
//
//        String[] urlToArray = null;
//        Matcher matcher = pattern.matcher(site.getUrl());
//        if (matcher.matches()) {
//            urlToArray = site.getUrl().split("www.");
//        }
//
//        if (urlToArray == null) {
//            siteUrl = site.getUrl();
//        } else {
//            StringBuilder stringBuilder = new StringBuilder();
//            for (String word : urlToArray) {
//                stringBuilder.append(word);
//            }
//            siteUrl = stringBuilder.toString();
//        }
//
//        return siteUrl;
//    }
}