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
import searchengine.dto.search.SearchRsDto;
import searchengine.exceptions.BadRequestException;
import searchengine.exceptions.SearchException;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.variables.IndexProcessVariables;

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
    private final ExceptionProperties exceptionProperties;

    private final LemmaFinderSettings lemmaFinderSettings;
    private final ConnectionSettings connectionSettings;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public SearchRsDto search(String query, String site, int offset, int limit) {
        log.info("search > начал работу: {}", IndexProcessVariables.isRUNNING());
        if (query.equals(" ") || offset < 0 || limit < 1) {
            throw new BadRequestException(exceptionProperties.getExceptionMessages().getBadRequest());
        }

        if (query.isEmpty()) {
            throw new SearchException(indexProperties.getMessages().getSearchError());
        }
        if (site != null && !isIndexed(site)) {
            throw new SearchException(indexProperties.getMessages().getSiteNotFound());
        }
        if (site == null && !isSitesIndexed()) {
            throw new SearchException(indexProperties.getMessages().getSitesNotIndexing());
        }
        IndexProcessVariables.setRUNNING(true);

        SearchRsDto searchRsDto = new SearchRsDto();
        searchRsDto.setResult(true);

        List<SiteEntity> siteList = new ArrayList<>();
        if (site != null) {
            siteList.add(getSite(site));
        } else {
            siteList.addAll(siteRepository.findAll());
        }

        LemmaFinder lemmaFinder = new LemmaFinder(lemmaFinderSettings, connectionSettings,
                siteRepository, pageRepository, lemmaRepository, indexRepository);

        HashMap<String, Integer> lemmasMap = lemmaFinder.searchingLemmasAndTheirCount(query);

        List<LemmaEntity> lemmaEntityList = new ArrayList<>();

        for (Map.Entry<String, Integer> pair : lemmasMap.entrySet()) {
            List<LemmaEntity> lemmaEntities = Optional.ofNullable(
                            lemmaRepository.findAllByLemmaNameAndSiteIn(pair.getKey(), siteList))
                    .orElse(new ArrayList<>());

            lemmaEntityList.addAll(lemmaEntities.stream()
                    .filter(lemmaEntity -> hasOneWord(query))
                    .filter(lemmaEntity -> !hasOneWord(query) && lemmaEntity.getFrequency() < 50)
                    .collect(Collectors.toList()));
            lemmaEntityList.addAll(lemmaEntities);
        }

        Collections.sort(lemmaEntityList);

        Collection<PageEntity> pageList = new ArrayList<>();

        if (hasOneWord(query)) {
            for (LemmaEntity lemma : lemmaEntityList) {
                Collection<PageEntity> pageEntities = getPageList(lemma);
                pageList.addAll(pageEntities);
            }
            searchRsDto.setCount(pageList.size());
        } else {
            for (int i = 0; i < lemmaEntityList.size(); i++) {
                if (i == 0) {
                    pageList = getPageList(lemmaEntityList.get(i));
                }
                if (lemmaEntityList.get(i).equals(lemmaEntityList.get(i++))) {
                    continue;
                }

                Collection<PageEntity> currentPageList = getPageList(lemmaEntityList.get(i));

                pageList = CollectionUtils.retainAll(pageList, currentPageList);
            }
            searchRsDto.setCount(pageList.size());
        }

        if (!pageList.isEmpty()) {
            HashMap<PageEntity, Float> absolutValuePageMap = new HashMap<>();

            for (PageEntity page : pageList) {
                float absoluteValue = 0;

                List<IndexEntity> indexEntityList = indexRepository.findAllByPage(page);
                for (IndexEntity index : indexEntityList) {
                    absoluteValue += index.getCountOfLemmaForPage();
                }
                absolutValuePageMap.put(page, absoluteValue);
            }
            Optional<Map.Entry<PageEntity, Float>> maxEntry = absolutValuePageMap.entrySet()
                    .stream().max(Map.Entry.comparingByValue());
            Float maxAbsoluteValue = maxEntry.get().getValue();

            HashMap<PageEntity, Float> relativePageMap = new HashMap<>();
            for (Map.Entry<PageEntity, Float> pair : absolutValuePageMap.entrySet()) {
                relativePageMap.put(pair.getKey(), pair.getValue() / maxAbsoluteValue);
            }

            List<Data> dataList = new ArrayList<>();

            if (siteList.size() == 1) {
                dataList.addAll(getDataList(siteList.get(0), relativePageMap, query));
            } else {
                for (SiteEntity siteEntity : siteList) {
                    dataList.addAll(getDataList(siteEntity, relativePageMap, query));
                }
            }

            Comparator<Data> compareByRelevance = Comparator.comparing(Data::getRelevance);
            List<Data> sortedDataList = dataList.stream().sorted(compareByRelevance.reversed()).toList();

            List<Data> totalDataList = new ArrayList<>();

            if (offset > sortedDataList.size()) {
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
            searchRsDto.setData(totalDataList);
        } else {
            searchRsDto.setData(new ArrayList<>());
        }

        IndexProcessVariables.setRUNNING(false);
        log.info("search > завершился: {}", IndexProcessVariables.isRUNNING());
        return searchRsDto;
    }

    private List<Data> getDataList(SiteEntity site, HashMap<PageEntity, Float> pageMap, String query) {
        List<Data> dataList = new ArrayList<>();

        for (Map.Entry<PageEntity, Float> pair : pageMap.entrySet()) {
            Connection.Response response = getConnectToUrl(site.getSiteUrl() + pair.getKey().getPagePath());
            if (response.statusCode() >= 400) {
                continue;
            }

            Data data = new Data()
                    .setSiteUrl(site.getSiteUrl())
                    .setSiteName(site.getSiteName())
                    .setUri(pair.getKey().getPagePath())
                    .setTitle(getTitle(site.getSiteUrl(), pair.getKey()))
                    .setSnippet(getSnippet(query, site.getSiteUrl(), pair.getKey()))
                    .setRelevance(pair.getValue());

            dataList.add(data);
            log.info("Добавили объект в dataList: {}", data);
        }
        return dataList;
    }

    private boolean hasOneWord(String query) {
        query = query.trim();
        return !query.contains(" ");
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
        String[] documentToArray = document.split("[.!?]");
        Set<String> sentenceSet = new HashSet<>(List.of(documentToArray));

        String[] queryToArray;
        if (hasOneWord(query)) {
            queryToArray = new String[]{query};
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
        if (sentenceSet.isEmpty()) {
            return "Заданного слова уже нет на странице";
        }

        String sentence = sentenceSet.iterator().next().trim();

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
                if (word.equals(string) || (word + ",").equals(string)) {
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
