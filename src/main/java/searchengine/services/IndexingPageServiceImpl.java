package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.indexing.IndexingPageResponse;
import searchengine.exceptions.IndexingPageException;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IndexingPageServiceImpl implements IndexingPageService {

    private final IndexProperties indexProperties;
    private final ConnectionSettings connectionSettings;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final LemmaFinderSettings lemmaFinderSettings;

    @Override
    public IndexingPageResponse startIndexingPage(String pagePath) {
        if (!hasSiteInConfiguration(pagePath)) {
            throw new IndexingPageException(indexProperties.getMessages().getIndexPageError());
        }

        IndexingPageResponse indexingPageResponse = new IndexingPageResponse();
        indexingPageResponse.setResult(true);

        if (siteRepository.findBySiteUrl(getSiteEntity(pagePath).getSiteUrl()) == null) {
            SiteEntity siteEntity = new SiteEntity()
                    .setStatusIndexing(StatusType.INDEXED)
                    .setStatusTime(LocalDateTime.now())
                    .setSiteUrl(getSite(pagePath).getUrl())
                    .setSiteName(getSite(pagePath).getName());
            siteRepository.save(siteEntity);
        }

        Connection.Response pageResponse = getDocumentByPagePath(pagePath);

        if (!hasPagePathToDB(pagePath)) {
            PageEntity pageEntity = new PageEntity()
                    .setSiteEntity(getSiteEntity(pagePath))
                    .setPagePath(getPagePath(pagePath))
                    .setResponseCode(pageResponse.statusCode())
                    .setContentPage(pageResponse.body());
            pageRepository.save(pageEntity);
        }
        String textByPage = getTextByPage(pagePath);

        HashMap<String, Integer> pageLemmasMap = searchingLemmasAndTheirCount(textByPage);

        for (Map.Entry<String, Integer> pair : pageLemmasMap.entrySet()) {

            LemmaEntity lemmaEntity = new LemmaEntity()
                    .setSite(getSiteEntity(pagePath))
                    .setLemmaName(pair.getKey());
            if (hasLemmaInDB(lemmaEntity.getLemmaName())) {
                lemmaEntity.setCountOfWordsPerPage(lemmaEntity.getCountOfWordsPerPage() + 1);
            } else {
                lemmaEntity.setCountOfWordsPerPage(1);
            }
            lemmaRepository.save(lemmaEntity);

            indexRepository.save(new IndexEntity()
                    .setPage(getPageEntity(pagePath))
                    .setLemma(lemmaEntity)
                    .setCountOfLemmaForPage(pair.getValue()));
        }
        return indexingPageResponse;
    }

    public boolean hasPagePathToDB(String pagePath) {
        return pageRepository.findByPagePathAndSiteEntity(getPagePath(pagePath), getSiteEntity(pagePath)) != null;
    }

    public boolean hasSiteInConfiguration(String pagePath) {
        String siteDomain = getPageDomain(pagePath);
        //String siteDomain = getSiteByPagePath(pagePath).getUrl();
        List<Site> siteList = indexProperties.getSites();
        for (Site site : siteList) {
            if (site.getUrl().contains(siteDomain)) {
                return true;
            }
        }
        return false;
    }

//    public void saveLemmaAndIndexToDB(HashMap<String, Integer> pageLemmasMap, String pagePath) {
//        for (Map.Entry<String, Integer> pair : pageLemmasMap.entrySet()) {
//
//            LemmaEntity lemmaEntity = new LemmaEntity();
//            lemmaEntity.setSite(getSiteEntity(pagePath));
//            lemmaEntity.setLemmaName(pair.getKey());
//
//            if (hasLemmaInDB(lemmaEntity.getLemmaName())) {
//                lemmaEntity.setCountOfWordsPerPage(lemmaEntity.getCountOfWordsPerPage() + 1);
//            } else {
//                lemmaEntity.setCountOfWordsPerPage(1);
//            }
//            lemmaRepository.save(lemmaEntity);
//
//            indexRepository.save(new IndexEntity()
//                    .setPage(getPageEntity(pagePath))
//                    .setLemma(lemmaEntity)
//                    .setCountOfLemmaForPage(pair.getValue()));
//        }
//    }

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

        if (array.length > 2) {
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
        }
        return stringBuilder.toString();
    }

//    public void saveSiteToDB(Site site) {
//        siteRepository.save(new SiteEntity()
//                .setStatusIndexing(StatusType.INDEXING)
//                .setStatusTime(LocalDateTime.now())
//                .setSiteUrl(site.getUrl())
//                .setSiteName(site.getName()));
//    }

    @SneakyThrows
    public Connection.Response getDocumentByPagePath(String pagePath) {
        return Jsoup.connect(pagePath)
                .ignoreHttpErrors(true)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferrer())
                .execute();
    }

//    public void savePageToDB(String pagePath, Connection.Response pageResponse, SiteEntity siteEntity) {
//        pageRepository.save(new PageEntity()
//                .setSiteEntity(siteEntity)
//                .setPagePath(getPagePath(pagePath))
//                .setResponseCode(pageResponse.statusCode())
//                .setContentPage(pageResponse.body()));
//    }

    public String getPagePath(String pagePath) {
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

    public SiteEntity getSiteEntity(String pagePath) {
        return siteRepository.findBySiteUrl(getPageDomain(pagePath));
    }

    public String getTextByPage(String pagePath) {
        return Jsoup.parse(pagePath).text();
    }

    @SneakyThrows
    public HashMap<String, Integer> searchingLemmasAndTheirCount(String text) {
        HashMap<String, Integer> lemmasMap = new HashMap<>();

        String[] arrayWordsByText = convertingTextToArray(text);

        for (String word : arrayWordsByText) {

            if (word.isBlank()) {
                continue;
            }

            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            List<String> wordBaseForms = luceneMorph.getMorphInfo(word);

            if (checkComplianceWordToParticlesNames(wordBaseForms)) {
                continue;
            }

            List<String> wordNormalFormList = luceneMorph.getNormalForms(word);

            String wordInNormalForm = wordNormalFormList.get(0); // TODO почему берем только первое слово???

            if (lemmasMap.containsKey(wordInNormalForm)) {
                lemmasMap.put(wordInNormalForm, lemmasMap.get(wordInNormalForm) + 1);
            } else {
                lemmasMap.put(wordInNormalForm, 1);
            }
        }
        return lemmasMap;
    }

    public String[] convertingTextToArray(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    public boolean checkComplianceWordToParticlesNames(List<String> stringList) {
        for (String string : stringList) {
            for (ParticlesNames particleName : lemmaFinderSettings.getParticlesNamesList()) {
                if (string.toUpperCase().contains(particleName.getParticle())) {
                    return true;
                }
            }
        }
        return false;
    }

    public PageEntity getPageEntity(String pagePath) {
        return pageRepository.findByPagePath(getPagePath(pagePath));
    }

    public boolean hasLemmaInDB(String lemmaName) {
        return lemmaRepository.findByLemmaName(lemmaName) != null;
    }
}
