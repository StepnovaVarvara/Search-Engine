package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.LemmaFinderSettings;
import searchengine.config.ParticlesNames;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LemmaFinder {

    //private final ConnectionSettings connectionSettings;
    @Autowired
    private LemmaFinderSettings lemmaFinderSettings;
    //private final IndexProperties indexProperties;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    @SneakyThrows
    public HashMap<String, Integer> searchingLemmasAndTheirCount(String text) {
        HashMap<String, Integer> lemmasMap = new HashMap<>();

        String[] arrayWords = convertingTextToArray(text);

        for (String word : arrayWords) {

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
    public String getTextByPage(String pagePath) {
        return Jsoup.parse(pagePath).text();
    }
    public String[] convertingTextToArray(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    @SneakyThrows
    public void parsePageAndSaveEntitiesToDB(String pageUrl, Connection.Response pageResponse) {
//        Connection.Response pageResponse = Jsoup.connect(pageUrl)
//                                            .ignoreContentType(true)
//                                            .userAgent(connectionSettings.getUserAgent())
//                                            .referrer(connectionSettings.getReferrer())
//                                            .execute();

        PageEntity pageEntity = new PageEntity()
                .setSiteEntity(getSiteEntity(pageUrl))
                .setPagePath(getPagePath(pageUrl))
                .setResponseCode(pageResponse.statusCode())
                .setContentPage(pageResponse.body());
        pageRepository.save(pageEntity);

        String pageText = getTextByPage(pageUrl);

        HashMap<String, Integer> pageLemmasMap = searchingLemmasAndTheirCount(pageText);

        for (Map.Entry<String, Integer> pair : pageLemmasMap.entrySet()) {

            LemmaEntity lemmaEntity = new LemmaEntity()
                    .setSite(getSiteEntity(pageUrl))
                    .setLemmaName(pair.getKey());
            if (hasLemmaInDB(lemmaEntity.getLemmaName())) {
                lemmaEntity.setCountOfWordsPerPage(lemmaEntity.getCountOfWordsPerPage() + 1);
            } else {
                lemmaEntity.setCountOfWordsPerPage(1);
            }
            lemmaRepository.save(lemmaEntity);

            indexRepository.save(new IndexEntity()
                    .setPage(getPageEntity(pageUrl))
                    .setLemma(lemmaEntity)
                    .setCountOfLemmaForPage(pair.getValue()));
        }
    }
    public SiteEntity getSiteEntity(String pagePath) {
        return siteRepository.findBySiteUrl(getPageDomain(pagePath));
    }
    public PageEntity getPageEntity(String pagePath) {
        return pageRepository.findByPagePath(getPagePath(pagePath));
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
    public boolean hasLemmaInDB(String lemmaName) {
        return lemmaRepository.findByLemmaName(lemmaName) != null;
    }
    public String getPagePath(String pageUrl) {
        String[] array = pageUrl.split("\\/+");
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
//    public SiteEntity getSiteId(String pageUrl) {
//        List<Site> siteList = indexProperties.getSites();
//        SiteEntity siteEntity = null;
//        for (Site site : siteList) {
//            if (pageUrl.contains(site.getUrl())) {
//                siteEntity = siteRepository.findBySiteUrl(site.getUrl());
//            }
//        }
//        return siteEntity;
//    }
//    public PageEntity getPageId(String pageUrl) {
//        Iterable<PageEntity> pageEntities = pageRepository.findAll();
//        PageEntity pageEntity = null;
//        for (PageEntity page : pageEntities) {
//            if (pageUrl.contains(page.getPagePath())) {
//                pageEntity = pageRepository.findByPagePath(page.getPagePath());
//            }
//        }
//        return pageEntity;
//    }
}

