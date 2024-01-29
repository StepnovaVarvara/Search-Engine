package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.config.ConnectionSettings;
import searchengine.config.IndexProperties;
import searchengine.config.LemmaFinderSettings;
import searchengine.config.Site;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.variables.IndexProcessVariables;

import java.time.LocalDateTime;
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
    private Connection.Response response;
    private String siteUrl;

    public PageRecursiveTask(String page, SiteRepository siteRepository, PageRepository pageRepository,
                             LuceneMorphology luceneMorphology, LemmaRepository lemmaRepository,
                             IndexRepository indexRepository, LemmaFinderSettings lemmaFinderSettings, boolean choose, int siteId, Connection.Response response, String siteUrl) {
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
        this.siteUrl = siteUrl;
    }

    public PageRecursiveTask(Site site, ConnectionSettings connectionSettings, SiteRepository siteRepository,
                             IndexProperties indexProperties, PageRepository pageRepository, boolean choose) {
        this.site = site;
        this.connectionSettings = connectionSettings;
        this.siteRepository = siteRepository;
        this.indexProperties = indexProperties;
        this.pageRepository = pageRepository;
        this.choose = choose;
    }

    @SneakyThrows
    @Override
    protected void compute() {
        log.info("Тут находимся: {}", this);
        if (choose) {
            log.info("Вот попали сюда: {}", this);
            //List<PageRecursiveTask> taskList = new ArrayList<>();

            Connection.Response siteResponse = getDocumentByUrl(site.getUrl());

            if (siteResponse.statusCode() == 200) {
                Document document = siteResponse.parse();

                SiteEntity siteEntity = new SiteEntity()
                        .setSiteName(site.getName())
                        .setSiteUrl(site.getUrl())
                        .setStatusIndexing(StatusType.INDEXING)
                        .setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity);

                Elements elements = document.select("a");
                for (Element element : elements) {
                    if (!IndexProcessVariables.isRUNNING()) {
                        siteEntity.setStatusIndexing(StatusType.FAILED);
                        siteEntity.setTextOfLastError(indexProperties.getMessages().getStop());
                        break;
                    } else {
                        Thread.sleep(500);

                        String page = element.attr("href");
                        if (page.startsWith("/") && !page.contains("#")) {
                            log.info("Мы до проверки: {}", this);
                            if (hasPageToDB(page, siteEntity)) {
                                log.info("Мы после проверки: {}", this);

                                // TODO создаем массив с урлами страниц

                                Connection.Response pageResponse = getDocumentByUrl(site.getUrl() + page);

                                if (pageResponse.statusCode() < 400) {
                                    log.info("Дошли до странички: {}", this);
                                    PageRecursiveTask pageRecursiveTask = new PageRecursiveTask(site.getUrl() + page, siteRepository, pageRepository, luceneMorphology, lemmaRepository, indexRepository, lemmaFinderSettings, false, siteEntity.getId(), pageResponse, site.getUrl());
                                    pageRecursiveTask.fork();
                                    pageRecursiveTask.join();

//                                    LemmaFinder lemmaFinder = new LemmaFinder(connectionSettings,
//                                            lemmaFinderSettings, siteRepository,
//                                            pageRepository, lemmaRepository, indexRepository);
//
//                                    lemmaFinder.parsePageAndSaveEntitiesToDB(site.getUrl() + page);
//
//                                    siteRepository.save(siteEntity.setStatusTime(LocalDateTime.now()));

                                    //log.info("Имя текущего потока: {}", Thread.currentThread().getName());
                                    //taskList.add(pageRecursiveTask);
                                } else {
                                    siteEntity.setStatusIndexing(StatusType.FAILED);
                                    siteEntity.setTextOfLastError(pageResponse.statusMessage());
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
//            for (PageRecursiveTask task : taskList) {
//                task.join();
//            }
        } else {

            LemmaFinder lemmaFinder = new LemmaFinder();
            log.info("Мы попали сюда: {}", lemmaFinder);

            lemmaFinder.parsePageAndSaveEntitiesToDB(siteUrl + page, response);

            SiteEntity siteEntity = siteRepository.findById(siteId).get();
            siteRepository.save(siteEntity.setStatusTime(LocalDateTime.now()));
        }
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

    private boolean hasPageToDB(String page, SiteEntity siteEntity) {

        PageEntity byPagePathAndSiteEntity = pageRepository.findByPagePathAndSiteEntity(page, siteEntity);
        log.info("Переменная page: {}", byPagePathAndSiteEntity);

        return byPagePathAndSiteEntity == null;
        // return pageRepository.findByPagePathAndSiteEntity(page, siteEntity) == null;
    }

}