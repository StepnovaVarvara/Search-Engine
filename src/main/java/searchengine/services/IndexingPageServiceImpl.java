package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.indexPage.IndexingPageRsDto;
import searchengine.dto.indexing.PageRsDto;
import searchengine.exceptions.BadRequestException;
import searchengine.exceptions.IndexingPageException;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.variables.IndexProcessVariables;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingPageServiceImpl implements IndexingPageService {

    private final IndexProperties indexProperties;
    private final ExceptionProperties exceptionProperties;
    private final ConnectionSettings connectionSettings;
    private final LemmaFinderSettings lemmaFinderSettings;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public IndexingPageRsDto startIndexingPage(String pagePath) {
        try {
            new URL(pagePath);
        } catch (MalformedURLException e) {
            throw new BadRequestException(exceptionProperties.getExceptionMessages().getBadRequest());
        }

        if (!hasSiteInConfiguration(pagePath)) {
            throw new IndexingPageException(indexProperties.getMessages().getIndexPageError());
        } else {
            IndexProcessVariables.setRUNNING(true);
            log.info("startIndexingPage > начал работу: {}", IndexProcessVariables.isRUNNING());

            IndexingPageRsDto indexingPageRsDto = new IndexingPageRsDto();
            indexingPageRsDto.setResult(true);

            PageRsDto pageRsDto = new PageRsDto(getDocumentByUrl(pagePath));
            if (pageRsDto.getStatusCode() < 400) {

                if (hasPageInDB(getLink(pagePath))) {
                    deleteLemmaEntityFromDB(pagePath);
                    indexRepository.deleteAllByPage(pageRepository.findByPagePath(getLink(pagePath)));
                    pageRepository.delete(pageRepository.findByPagePath(getLink(pagePath)));
                }

                if (!hasSiteInDB(pagePath)) {
                    SiteEntity siteEntity = new SiteEntity()
                            .setStatusIndexing(StatusType.INDEXED)
                            .setStatusTime(LocalDateTime.now())
                            .setSiteUrl(getSite(pagePath).getUrl())
                            .setSiteName(getSite(pagePath).getName());
                    siteRepository.save(siteEntity);
                }

                LemmaFinder lemmaFinder = new LemmaFinder(lemmaFinderSettings, connectionSettings, siteRepository,
                        pageRepository, lemmaRepository, indexRepository);

                lemmaFinder.parsePageAndSaveEntitiesToDB(pagePath, pageRsDto, getSiteEntity(pagePath).getId());
            }

            IndexProcessVariables.setRUNNING(false);
            log.info("startIndexingPage > завершился: {}", IndexProcessVariables.isRUNNING());
            return indexingPageRsDto;
        }
    }
    public void deleteLemmaEntityFromDB(String pagePath) {
        PageEntity pageEntity = pageRepository.findByPagePath(getLink(pagePath));

        List<IndexEntity> indexEntityList = indexRepository.findAllByPage(pageEntity);
        for (IndexEntity indexEntity : indexEntityList) {
            lemmaRepository.deleteById(indexEntity.getLemma().getId());
        }
    }
    public boolean hasPageInDB(String pagePath) {
        return pageRepository.findByPagePath(pagePath) != null;
    }
    public String getLink(String pagePath) {
        String[] array = pagePath.split("\\/+");
        StringBuilder stringBuilder = new StringBuilder();

        if (array.length > 2) {
            for (int i = 0; i < array.length; i++) {
                if (i <= 1) {
                    continue;
                }
                stringBuilder.append("/" + array[i]);
            }
        } else {
            stringBuilder.append("/");
        }
        return stringBuilder.toString();
    }

    @SneakyThrows
    public Connection.Response getDocumentByUrl(String url) {
        return Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferrer())
                .execute();
    }

    public boolean hasSiteInConfiguration(String pagePath) {
        String siteDomain = getPageDomain(pagePath);
        List<Site> siteList = indexProperties.getSites();
        for (Site site : siteList) {
            if (site.getUrl().equals(siteDomain)) {
                return true;
            }
        }
        return false;
    }

    public Site getSite(String pagePath) {
        Site currentSite = null;
        String siteDomain = getPageDomain(pagePath);

        List<Site> siteList = indexProperties.getSites();
        for (Site site : siteList) {
            if (site.getUrl().equals(siteDomain)) {
                currentSite = site;
            }
        }
        return currentSite;
    }

    public String getPageDomain(String pagePath) {
        String[] array = pagePath.split("\\/+");
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < array.length; i++) {
            if (i >= 2) {
                break;
            }
            if (i == 0) {
                stringBuilder.append(array[i] + "//");
            } else {
                stringBuilder.append(array[i]);
            }
        }
        return stringBuilder.toString();
    }

    public boolean hasSiteInDB(String pagePath) {
        return siteRepository.findBySiteUrl(getPageDomain(pagePath)) != null;
    }

    public SiteEntity getSiteEntity(String pagePath) {
        return siteRepository.findBySiteUrl(getPageDomain(pagePath));
    }
}
