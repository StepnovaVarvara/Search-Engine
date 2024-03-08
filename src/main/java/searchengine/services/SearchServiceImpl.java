package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.search.Data;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.SearchException;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final IndexProperties indexProperties;
    private final StatisticsProperties statisticsProperties;

    private final LemmaFinderSettings lemmaFinderSettings;
    private final ConnectionSettings connectionSettings;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        if (query.isEmpty()) {
            throw new SearchException(indexProperties.getMessages().getSearchError());
        }
        if (site != null && !isIndexed(site)) {
            throw new SearchException(indexProperties.getMessages().getSiteNotFound());
        }
        if (site == null && !isSitesIndexed()) {
            throw new SearchException(indexProperties.getMessages().getSitesNotIndexing());
        }

        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        log.info("Запустились!");

        //сформировать лист сайтов
        List<Integer> siteIdList = null; //Zdelat kak to

        LemmaFinder lemmaFinder = new LemmaFinder(lemmaFinderSettings, connectionSettings,
                siteRepository, pageRepository, lemmaRepository, indexRepository);

        // получили список лемм из запроса
        HashMap<String, Integer> lemmasMap = lemmaFinder.searchingLemmasAndTheirCount(query);
        log.info("Длина мапы: {}", lemmasMap.size());

        List<LemmaEntity> lemmaEntityList = new ArrayList<>();

        for (Map.Entry<String, Integer> pair : lemmasMap.entrySet()) {
            List<LemmaEntity> lemmaEntities = Optional.ofNullable(
                    //lemmaRepository.findByLemmaName(pair.getKey(),siteIdList) //TODO
                    //SELECT * TABLE aa WHERE aa.id in ("1","2","3") and aaa.
                    lemmaRepository.findByLemmaNameAndSites(pair.getKey(),siteIdList))
                    .orElse(new ArrayList<>());
            //LemmaEntity lemmaEntity = lemmaRepository.findByLemmaName(pair.getKey());

            lemmaEntityList.addAll(lemmaEntities.stream()
                    .filter(lemmaEntity -> hasOneWord(query) && lemmaEntity.getCountOfWordsPerPage() > 100)
                    .filter(lemmaEntity -> lemmaEntity.getCountOfWordsPerPage() < 100)
                    .collect(Collectors.toList()));


//            if (lemmaEntity) {
//                continue;
//            }
//            if (hasOneWord(query) && lemmaEntity.get().getCountOfWordsPerPage() > 100) {
//                lemmaEntityList1.add(lemmaEntity.get());
//            }
//            if (lemmaEntity.get().getCountOfWordsPerPage() < 100) {
//                lemmaEntityList1.add(lemmaEntity.get());
//            }
        }
        // получили отсортированный лист лемм
        Collections.sort(lemmaEntityList);

        //
        Collection<PageEntity> pageList = new ArrayList<>();

        for (int i = 0; i < lemmaEntityList.size(); i++) {
            if (i == 0) {
                pageList = getPageList(lemmaEntityList.get(i));
            }
            Collection<PageEntity> currentPageList = getPageList(lemmaEntityList.get(i));
            pageList = CollectionUtils.retainAll(pageList, currentPageList);
        }
        // получили лист пейджей
        searchResponse.setCount(pageList.size());

        if (!pageList.isEmpty()) {
            HashMap<PageEntity, Float> absolutValuePageMap = new HashMap<>();

            for (PageEntity page : pageList) {
                float absoluteValue = 0;

                List<IndexEntity> indexEntityList = indexRepository.findAllByPage(page);
                for (IndexEntity index : indexEntityList) {
                    absoluteValue += index.getCountOfLemmaForPage();
                }
                // получили мапу пейджей с абсолютной релевантностью
                absolutValuePageMap.put(page, absoluteValue);
            }
            Optional<Map.Entry<PageEntity, Float>> maxEntry = absolutValuePageMap.entrySet()
                    .stream().max(Map.Entry.comparingByValue());
            Float relativeValue = maxEntry.get().getValue();

            HashMap<PageEntity, Float> relativePageMap = new HashMap<>();
            for (Map.Entry<PageEntity, Float> pair : absolutValuePageMap.entrySet()) {
                // получили мапу пейджей с относительной релевантностью
                relativePageMap.put(pair.getKey(), pair.getValue() / relativeValue);
            }
            log.info("Размер мапы: {}", relativePageMap.size());

            List<Data> dataList = new ArrayList<>();
            log.info("Создали dataList");


            //Создаем список сайтов

//            List<Site> siteList = new ArrayList<>();
//List<Data> dataList1 =  new ArrayList<>();
//
//            if (site != null) {
//                siteList.add(null);//site достать из пропертей)
//            } else {
//                siteList = indexProperties.getSites();
//            }
//
//            for (Site currentSite : siteList) {
//                //List<Data> currentDataList = getDataList(currentSite.getUrl(), relativePageMap, query);
//                dataList1.addAll(getDataList(currentSite.getUrl(), relativePageMap, query));
//            }
//

            if (site != null) {
                dataList = getDataList(site, relativePageMap, query);
            } else {
                List<Site> siteList = indexProperties.getSites();
                for (Site currentSite : siteList) {
                    //List<Data> currentDataList = getDataList(currentSite.getUrl(), relativePageMap, query);
                    dataList.addAll(getDataList(currentSite.getUrl(), relativePageMap, query));
                }
            }

//            List<Data> dataList = new ArrayList<>();
//            for (Map.Entry<PageEntity, Float> pair : relativePageMap.entrySet()) {
//                Connection.Response response = getConnectToUrl(getSite(pair.getKey()).getSiteUrl() + pair.getKey().getPagePath());
//                if (response.statusCode() >= 400) {
//                    continue;
//                }
//                log.info("Страница из мапы: {}", pair.getKey().getPagePath());
//                Data data = new Data()
//                        .setSiteUrl(getSite(pair.getKey()).getSiteUrl()) // TODO а если сайтов много?
//                        .setSiteName(getSite(pair.getKey()).getSiteName())
//                        .setUri(pair.getKey().getPagePath())
//                        .setTitle(getTitle(getSite(pair.getKey()).getSiteUrl()))
//                        .setSnippet(getSnippet(query, getSite(pair.getKey()).getSiteUrl(), pair.getKey()))
//                        .setRelevance(pair.getValue());
//
//                dataList.add(data);
//                log.info("Добавили объект в dataList: {}", data);
//            }

            Comparator<Data> compareByRelevance = Comparator.comparing(Data::getRelevance);
            List<Data> sortedDataList = dataList.stream().sorted(compareByRelevance.reversed()).toList();

            List<Data> totalDataList = new ArrayList<>();

        if (offset > sortedDataList.size()) { // TODO разобраться с show more
            throw new SearchException(indexProperties.getMessages().getOffsetError());
        }
            if (limit > sortedDataList.size() - offset) {
                for (int i = offset; i < sortedDataList.size(); i++) {
                    totalDataList.add(sortedDataList.get(i));
                }
            } else {
                for (int i = offset; i < limit; i++) {
                    totalDataList.add(sortedDataList.get(i));
                }
            }
            searchResponse.setData(totalDataList);
        } else {
            List<Data> totalDataList = new ArrayList<>();
            searchResponse.setData(totalDataList);
        }

        return searchResponse;
    }

    private List<Data> getDataList(String site, HashMap<PageEntity, Float> pageMap, String query) {
        List<Data> dataList = new ArrayList<>();

        for (Map.Entry<PageEntity, Float> pair : pageMap.entrySet()) {
            Connection.Response response = getConnectToUrl(site + pair.getKey().getPagePath());
            if (response.statusCode() >= 400) {
                continue;
            }
            log.info("Страница из мапы: {}", pair.getKey().getPagePath());
            Data data = new Data()
                    .setSiteUrl(getSite(site).getSiteUrl())
                    .setSiteName(getSite(site).getSiteName())
                    .setUri(pair.getKey().getPagePath())
                    .setTitle(getTitle(site, pair.getKey()))
                    .setSnippet(getSnippet(query, site, pair.getKey()))
                    .setRelevance(pair.getValue());

            dataList.add(data);
            log.info("Добавили объект в dataList: {}", data);
        }
        return dataList;
    }

    private boolean hasOneWord(String query) {
        query = query.trim();
        if (!query.contains(" ")) {
            return true;
        }
        return false;
    }

    private SiteEntity getSite(String site) {
        return siteRepository.findBySiteUrl(site);
    }

    private boolean isIndexed(String site) {
        SiteEntity siteEntity = siteRepository.findBySiteUrl(site);
        return siteEntity.getStatusIndexing().toString().equals(statisticsProperties.getStatuses().getIndexed());
    }

    private boolean isSitesIndexed() {
        List<SiteEntity> indexedSiteList = new ArrayList<>();

        List<SiteEntity> siteEntityList = siteRepository.findAll();
        for (SiteEntity site : siteEntityList) {
            if (site.getStatusIndexing().toString().equals(statisticsProperties.getStatuses().getIndexed())) {
                indexedSiteList.add(site);
            }
        }
        return indexedSiteList.size() == indexProperties.getSites().size();
    }

    private Collection<PageEntity> getPageList(LemmaEntity lemmaEntity) {
        List<IndexEntity> indexEntityList = indexRepository.findAllByLemma(lemmaEntity);

        Collection<PageEntity> pageList = new ArrayList<>();

        for (IndexEntity indexEntity : indexEntityList) {
            PageEntity pageEntity = pageRepository.findById(indexEntity.getPage().getId());
            pageList.add(pageEntity);
        }
        return pageList;
    }

    @SneakyThrows
    private Connection.Response getConnectToUrl(String url) {
        return Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferrer())
                .execute();
    }

    @SneakyThrows
    private String getTitle(String site, PageEntity page) {
        Connection.Response response = getConnectToUrl(site + page.getPagePath());

        Document document = response.parse();

        String title = null;
        Elements elements = document.select("title");
        for (Element element : elements) {
            title = element.text();
        }
        return title;
    }

    @SneakyThrows
    private String getSnippet(String query, String site, PageEntity page) {
        Connection.Response response = getConnectToUrl(site + page.getPagePath());

        String document = response.parse().text().toLowerCase();
        String[] documentToArray = document.split("\\.");
        Set<String> sentenceSet = new HashSet<>(List.of(documentToArray));
        log.info("Размер сета: {}", sentenceSet.size());

        String[] queryToArray;
        if (hasOneWord(query)) {
            queryToArray = new String[] {query};
        } else {
            queryToArray = query.split(" ");
        }

        for (String word : queryToArray) {
            String regex = ".*\\b" + word + "\\b.*";
            Pattern pattern = Pattern.compile(regex);

            Iterator<String> iterator = sentenceSet.iterator();
            while (iterator.hasNext()) {
                String nextSentence = iterator.next();
                Matcher matcher = pattern.matcher(nextSentence);
                if (!matcher.matches()) {
                    iterator.remove();
                }
            }
        }
        log.info("Размер оставшегося сета: {}", sentenceSet.size());
        if (sentenceSet.isEmpty()) {
            return "Заданного слова уже нет на странице";
        }

        String sentence = sentenceSet.iterator().next().trim();
        log.info("Предложение из итератора: {}", sentence);

        String[] sentenceToArray = sentence.split(" ");

        StringBuilder stringBuilder = getBoldFont(sentenceToArray, queryToArray);

        return stringBuilder.toString().trim();
    }

    private StringBuilder getBoldFont(String[] sentenceToArray, String[] queryToArray) {
        ArrayList<String> wordList = new ArrayList<>(List.of(sentenceToArray));

        for (String word : queryToArray) {
            ListIterator<String> iteratorList = wordList.listIterator();
            while (iteratorList.hasNext()) {
                String string = iteratorList.next();
                if (word.equals(string)) {
                    iteratorList.set("<b>" + word + "</b>");
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String word : wordList) {
            stringBuilder.append(word + " ");
        }

        return stringBuilder;
    }
}
