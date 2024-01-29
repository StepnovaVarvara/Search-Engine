package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionSettings;
import searchengine.config.Site;
import searchengine.config.IndexProperties;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.IndexingException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
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

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final ConnectionSettings connectionSettings;
    private final IndexProperties indexProperties;
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(10);

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
                PageRecursiveTask pageRecursiveTask = new PageRecursiveTask(site, connectionSettings, siteRepository, indexProperties, pageRepository, true);
                forkJoinPool.invoke(pageRecursiveTask);
            } else {
                saveSiteToDB(site, StatusType.FAILED, indexProperties.getMessages().getStop());
            }
        }

        Date finish = new Date();
        log.info("Index finish with time: {} ms", (finish.getTime() - start.getTime()));

        IndexProcessVariables.setRUNNING(false);

//                Connection.Response siteResponse = getDocumentByUrl(site.getUrl());
//
//                if (siteResponse.statusCode() == 200) {
//                    Document document = siteResponse.parse();
//                    SiteEntity siteEntity = new SiteEntity()
//                            .setSiteName(site.getName())
//                            .setSiteUrl(site.getUrl())
//                            .setStatusIndexing(StatusType.INDEXING)
//                            .setStatusTime(LocalDateTime.now());
//                    siteRepository.save(siteEntity);
//
//                    Elements elements = document.select("a");
//                    for (Element element : elements) {
//                        if (!IndexProcessVariables.isRUNNING()) {
//                            siteEntity.setStatusIndexing(StatusType.FAILED);
//                            siteEntity.setTextOfLastError(indexProperties.getMessages().getStop());
//                            break;
//                        } else {
//                            Thread.sleep(500);
//
//                            String link = element.attr("href");
//                            if (link.startsWith("/") && !link.contains("#") && !link.equals(hasLinkToDB(link, siteEntity))) {
//                                log.info("Site: {}, Page: {}", site, link);
//
//                                Connection.Response response = getDocumentByUrl(site.getUrl() + link);
//                                if (response.statusCode() < 400) {
//                                    //PageRecursive pageRecursive = new PageRecursive(link, response.statusCode(), response.body(), siteEntity, siteRepository, pageRepository);
//                                    forkJoinPool.invoke(pageRecursive);
//                                    log.info("Running thread: {}", forkJoinPool.getRunningThreadCount());
//                                    log.info("Active thread: {}", forkJoinPool.getActiveThreadCount());
////                                    FJP.getInstance().invoke(pageRecursive);
////                                    log.info("Running thread: {}", FJP.getInstance().getRunningThreadCount());
////                                    log.info("Active thread: {}", FJP.getInstance().getActiveThreadCount());
//                                } else {
//                                    siteEntity.setStatusIndexing(StatusType.FAILED);
//                                    siteEntity.setTextOfLastError(response.statusMessage());
//                                }
//                            }
//                        }
//                    }
//                    if (IndexProcessVariables.isRUNNING()) {
//                        siteRepository.save(siteEntity.setStatusIndexing(StatusType.INDEXED));
//                    }
//                }
//                if (siteResponse.statusCode() >= 400) {
//                    saveSiteToDB(site, StatusType.FAILED, siteResponse.statusMessage());
//                }

    }

    @SneakyThrows
    public Connection.Response getDocumentByUrl(String url) {
        return Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferrer())
                .execute();
    }

    private void saveSiteToDB(Site site, StatusType statusType, String textOfLastError) {
        siteRepository.save(new SiteEntity()
                .setSiteName(site.getName())
                .setSiteUrl(site.getUrl())
                .setStatusIndexing(statusType)
                .setTextOfLastError(textOfLastError)
                .setStatusTime(LocalDateTime.now()));
    }
    private String hasLinkToDB(String link, SiteEntity siteEntity) {
        Iterable<PageEntity> pageEntityIterable = pageRepository.findAll();
        String currentPageEntityPath = null;
        for (PageEntity pageEntity : pageEntityIterable) {
            if (pageEntity.getPagePath().equals(link) && pageEntity.getSiteEntity().equals(siteEntity)) {
                currentPageEntityPath = pageEntity.getPagePath();
            }
        }
        return currentPageEntityPath;
    }
}