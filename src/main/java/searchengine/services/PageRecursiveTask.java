package searchengine.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.*;
import searchengine.dto.indexing.PageResponse;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.variables.FJP;
import searchengine.variables.IndexProcessVariables;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class PageRecursiveTask extends RecursiveAction {
    private Site site;
    private String page;
    private ConnectionSettings connectionSettings;
    private SiteRepository siteRepository;
    private IndexProperties indexProperties;
    private PageRepository pageRepository;
    private LuceneMorphology luceneMorphology;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private LemmaFinderSettings lemmaFinderSettings;
    private PageRecursiveTaskProperties pageRecursiveTaskProperties;
    private boolean isSite;
    private int siteId;
    private PageResponse response;
    private FJP fjp;

    public PageRecursiveTask(String page, SiteRepository siteRepository, PageRepository pageRepository,
                             LuceneMorphology luceneMorphology, LemmaRepository lemmaRepository,
                             IndexRepository indexRepository, LemmaFinderSettings lemmaFinderSettings,
                             boolean isSite, int siteId, PageResponse response,
                             ConnectionSettings connectionSettings, PageRecursiveTaskProperties pageRecursiveTaskProperties, FJP fjp) {
        this.page = page;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.luceneMorphology = luceneMorphology;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinderSettings = lemmaFinderSettings;
        this.isSite = isSite;
        this.siteId = siteId;
        this.response = response;
        this.connectionSettings = connectionSettings;
        this.pageRecursiveTaskProperties = pageRecursiveTaskProperties;
        this.fjp = fjp;
    }

    public PageRecursiveTask(Site site, ConnectionSettings connectionSettings, SiteRepository siteRepository,
                             IndexProperties indexProperties, PageRepository pageRepository, boolean isSite,
                             LemmaRepository lemmaRepository, IndexRepository indexRepository,
                             LemmaFinderSettings lemmaFinderSettings, PageRecursiveTaskProperties pageRecursiveTaskProperties) {
        this.site = site;
        this.connectionSettings = connectionSettings;
        this.siteRepository = siteRepository;
        this.indexProperties = indexProperties;
        this.pageRepository = pageRepository;
        this.isSite = isSite;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinderSettings = lemmaFinderSettings;
        this.pageRecursiveTaskProperties = pageRecursiveTaskProperties;
    }

    @SneakyThrows
    @Override
    protected void compute() {
        if (isSite) {
            List<PageRecursiveTask> taskList = new ArrayList<>();

            Connection.Response siteResponse = getConnectToUrl(site.getUrl());

            if (siteResponse.statusCode() == 200) {
                Document document = siteResponse.parse();

                SiteEntity siteEntity = new SiteEntity()
                        .setSiteName(site.getName())
                        .setSiteUrl(site.getUrl())
                        .setStatusIndexing(StatusType.INDEXING)
                        .setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity);

                Set<String> pageSet = new HashSet<>();
                Elements elements = document.select("a");
                for (Element element : elements) {
                    if (!IndexProcessVariables.isRUNNING()) {
                        siteEntity.setStatusIndexing(StatusType.FAILED);
                        siteEntity.setTextOfLastError(indexProperties.getMessages().getStop());
                        log.info("Время выхода из таски > {}", new Date());
                        break;
                    } else {
                        Thread.sleep(500);

                        String page = element.attr("href");
                        if (page.endsWith("/") && page.length() != 1) {
                            page = StringUtils.substring(page, 0, page.length() - 1);
                            // TODO необязаельная проверка, иногда попадаются одинаковые page,
                            //  отличающиеся только слешом в конце (например, /contacts и /contacts/)
                        }
                        if (page.startsWith("/") && !page.contains("#") && !hasExtension(page) && !pageSet.contains(page)) {
                            if (!hasPageInDB(page, siteEntity)) {
                                pageSet.add(page);

                                PageResponse pageResponse = new PageResponse(getConnectToUrl(site.getUrl() + page));

                                if (pageResponse.getStatusCode() < 400) {
                                    PageRecursiveTask pageRecursiveTask = new PageRecursiveTask(site.getUrl() + page, siteRepository,
                                            pageRepository, luceneMorphology, lemmaRepository, indexRepository, lemmaFinderSettings,
                                            false, siteEntity.getId(), pageResponse, connectionSettings, pageRecursiveTaskProperties, fjp);
                                    pageRecursiveTask.fork();

                                    taskList.add(pageRecursiveTask);
                                } else {
                                    siteEntity.setStatusIndexing(StatusType.FAILED);
                                    siteEntity.setTextOfLastError(pageResponse.getStatusMessage());
                                }
                            }
                        }
                    }
                }
                if (IndexProcessVariables.isRUNNING()) {
                    siteRepository.save(siteEntity.setStatusIndexing(StatusType.INDEXED));
                }
            }
            if (siteResponse.statusCode() >= 400) {
                saveSiteToDB(site, StatusType.FAILED, siteResponse.statusMessage());
            }
            for (PageRecursiveTask task : taskList) {
                if (IndexProcessVariables.isRUNNING()) {
                    task.join();
                }
            }
        } else {
            if (IndexProcessVariables.isRUNNING()) {
                LemmaFinder lemmaFinder = new LemmaFinder(lemmaFinderSettings, connectionSettings,
                        siteRepository, pageRepository, lemmaRepository, indexRepository);

                lemmaFinder.parsePageAndSaveEntitiesToDB(page, response, siteId);

                SiteEntity siteEntity = siteRepository.findById(siteId).get();
                siteRepository.save(siteEntity.setStatusTime(LocalDateTime.now()));
            }
        }
    }

    @SneakyThrows
    public Connection.Response getConnectToUrl(String url) {
        return Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferrer())
                .execute();
    }

    public void saveSiteToDB(Site site, StatusType statusType, String textOfLastError) {
        siteRepository.save(new SiteEntity()
                .setSiteName(site.getName())
                .setSiteUrl(site.getUrl())
                .setStatusIndexing(statusType)
                .setTextOfLastError(textOfLastError)
                .setStatusTime(LocalDateTime.now()));
    }

    public boolean hasPageInDB(String page, SiteEntity siteEntity) {
        return pageRepository.findByPagePathAndSiteEntity(page, siteEntity) != null;
    }

    public boolean hasExtension(String page) {
        for (String extension : pageRecursiveTaskProperties.getExtensionsList()) {
            if (page.contains(extension)) {
                return true;
            }
        }
        return false;
    }
}