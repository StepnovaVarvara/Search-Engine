package searchengine.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.ConnectionSettings;
import searchengine.config.IndexProperties;
import searchengine.config.LemmaFinderSettings;
import searchengine.config.Site;
import searchengine.dto.indexing.PageResponse;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.variables.IndexProcessVariables;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private boolean choose;
    private int siteId;
    private PageResponse response;

    public PageRecursiveTask(String page, SiteRepository siteRepository, PageRepository pageRepository,
                             LuceneMorphology luceneMorphology, LemmaRepository lemmaRepository,
                             IndexRepository indexRepository, LemmaFinderSettings lemmaFinderSettings,
                             boolean choose, int siteId, PageResponse response,
                             ConnectionSettings connectionSettings) {
        this.page = page;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.luceneMorphology = luceneMorphology;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinderSettings = lemmaFinderSettings;
        this.choose = choose;
        this.siteId = siteId;
        this.response = response;
        this.connectionSettings = connectionSettings;
    }

    public PageRecursiveTask(Site site, ConnectionSettings connectionSettings, SiteRepository siteRepository,
                             IndexProperties indexProperties, PageRepository pageRepository, boolean choose,
                             LemmaRepository lemmaRepository, IndexRepository indexRepository, LemmaFinderSettings lemmaFinderSettings) {
        this.site = site;
        this.connectionSettings = connectionSettings;
        this.siteRepository = siteRepository;
        this.indexProperties = indexProperties;
        this.pageRepository = pageRepository;
        this.choose = choose;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinderSettings = lemmaFinderSettings;
    }

    @SneakyThrows
    @Override
    protected void compute() {
        if (choose) {
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

                Elements elements = document.select("a");
                // Elements - eto massiv
                // Set - eto massiv gde net povtoreniy
                HashMap<String, Element> aaaa = new HashMap<>();


                for (Element element : elements) {
                    if (!IndexProcessVariables.isRUNNING()) {
                        siteEntity.setStatusIndexing(StatusType.FAILED);
                        siteEntity.setTextOfLastError(indexProperties.getMessages().getStop());
                        break;
                    } else {
                        Thread.sleep(500);

                        String page = element.attr("href");
                        if (page.startsWith("/") && !page.contains("#")) {
                            if (!hasPageToDB(page, siteEntity)) { //TODO перепроверить IndexPage

                                PageResponse pageResponse = new PageResponse(getConnectToUrl(site.getUrl() + page));

                                if (pageResponse.getStatusCode() < 400) {
                                    PageRecursiveTask pageRecursiveTask = new PageRecursiveTask(site.getUrl() + page, siteRepository,
                                            pageRepository, luceneMorphology, lemmaRepository, indexRepository, lemmaFinderSettings,
                                            false, siteEntity.getId(), pageResponse, connectionSettings);
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
                    log.info("INDEXED");
                }
            }
            if (siteResponse.statusCode() >= 400) {
                saveSiteToDB(site, StatusType.FAILED, siteResponse.statusMessage());
            }
            for (PageRecursiveTask task : taskList) {
                task.join();
            }
        } else {
            LemmaFinder lemmaFinder = new LemmaFinder(lemmaFinderSettings, connectionSettings,
                    siteRepository, pageRepository, lemmaRepository, indexRepository);

                lemmaFinder.parsePageAndSaveEntitiesToDB(page, response, siteId);

            SiteEntity siteEntity = siteRepository.findById(siteId).get();
            siteRepository.save(siteEntity.setStatusTime(LocalDateTime.now()));
        }
//        if (IndexProcessVariables.isRUNNING()) {
//            siteRepository.save(siteRepository.findBySiteUrl(site.getUrl()).setStatusIndexing(StatusType.INDEXED));
//            log.info("INDEXED"); // TODO проверить работу!!!
//        }
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

    public boolean hasPageToDB(String page, SiteEntity siteEntity) {
        return pageRepository.findByPagePathAndSiteEntity(page, siteEntity) != null;
    }
}