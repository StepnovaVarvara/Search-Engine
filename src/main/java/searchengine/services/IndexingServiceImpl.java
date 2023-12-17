package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionSettings;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final ConnectionSettings connectionSettings;
    private final SitesList sites;

    @SneakyThrows
    public Connection.Response getDocumentByUrl(String url) {
        Connection.Response response = Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferrer())
                .execute();
        return response;
    }

    @SneakyThrows
    public IndexingResponse startIndexing() {
        Date start = new Date();

        IndexingResponse indexingResponse = new IndexingResponse();
        indexingResponse.setResult(true);

        pageRepository.deleteAll();
        siteRepository.deleteAll();

        List<Site> siteList = sites.getSites();
        for (Site url : siteList) {
            Document document = getDocumentByUrl(url.getUrl()).parse();

            SiteEntity siteEntity = new SiteEntity()
                    .setSiteName(url.getName())
                    .setSiteUrl(url.getUrl())
                    .setStatusIndexing(StatusType.INDEXING)
                    .setStatusTime(LocalDateTime.now());
            if (getDocumentByUrl(url.getUrl()).statusCode() < 400) {
                siteEntity.setTextOfLastError("NULL");
            } else {
                siteEntity.setTextOfLastError(getDocumentByUrl(url.getUrl()).statusMessage());
            }
            siteRepository.save(siteEntity);

            Elements elements = document.select("a");
            for (Element element : elements) {
                Thread.sleep(500);

                String link = element.attr("href");
                if (link.startsWith("/") && !link.contains("#")) {
                    Connection.Response response = getDocumentByUrl(url.getUrl() + link);
                    PageEntity page = new PageEntity()
                            .setSiteEntity(siteEntity)
                            .setPagePath(link)
                            .setResponseCode(response.statusCode())
                            .setContentPage(response.body());
                    pageRepository.save(page);

                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteEntity);
                }
            }
            siteEntity.setStatusIndexing(StatusType.INDEXED);
            siteRepository.save(siteEntity);
        }
        Date finish = new Date();
        System.out.println((finish.getTime() - start.getTime()) + " ms");

        return indexingResponse;
    }
}
