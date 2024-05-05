package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

        HashMap<String, Integer> queryLemmasMap = lemmaFinder.searchingLemmasAndTheirCount(query);

        List<LemmaEntity> lemmaEntityList = new ArrayList<>();

        for (Map.Entry<String, Integer> pair : queryLemmasMap.entrySet()) {
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

        List<PageEntity> pageList = new ArrayList<>();
        for (LemmaEntity lemma : lemmaEntityList) {
            List<PageEntity> pageEntities = getPageList(lemma);
            pageList.addAll(pageEntities);
        }

        if (!pageList.isEmpty()) {
            Map<PageEntity, Float> absolutValuePageMap = getAbsolutValuePageMap(pageList, lemmaEntityList);

            Optional<Map.Entry<PageEntity, Float>> maxEntry = absolutValuePageMap.entrySet()
                    .stream().max(Map.Entry.comparingByValue());
            Float maxAbsoluteValue = maxEntry.get().getValue();

            HashMap<PageEntity, Float> relativeValuePageMap = new HashMap<>();
            for (Map.Entry<PageEntity, Float> pair : absolutValuePageMap.entrySet()) {
                relativeValuePageMap.put(pair.getKey(), pair.getValue() / maxAbsoluteValue);
            }

            List<Data> dataList = new ArrayList<>();
            if (siteList.size() == 1) {
                dataList.addAll(getDataList(siteList.get(0), relativeValuePageMap, query));
            } else {
                for (SiteEntity siteEntity : siteList) {
                    dataList.addAll(getDataList(siteEntity, relativeValuePageMap, query));
                }
            }
            searchRsDto.setCount(dataList.size());

            Comparator<Data> compareByRelevance = Comparator.comparing(Data::getRelevance);
            List<Data> sortedDataList = dataList.stream().sorted(compareByRelevance.reversed()).toList();
            searchRsDto.setData(sortedDataList);

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

    private Map<PageEntity, Float> getAbsolutValuePageMap(List<PageEntity> pageList, List<LemmaEntity> lemmaEntityList) {
        HashMap<PageEntity, Float> absolutValuePageMap = new HashMap<>();
        for (PageEntity page : pageList) {
            float absoluteValue = 0;
            for (LemmaEntity lemma : lemmaEntityList) {
                List<IndexEntity> indexEntityList = indexRepository.findAllByPageAndLemma(page, lemma);
                for (IndexEntity index : indexEntityList) {
                    absoluteValue += index.getCountOfLemmaForPage();
                }
            }
            absolutValuePageMap.put(page, absoluteValue);
        }

        return absolutValuePageMap;
    }

    private List<Data> getDataList(SiteEntity site, HashMap<PageEntity, Float> pageMap, String query) {
        List<Data> dataList = new ArrayList<>();

        for (Map.Entry<PageEntity, Float> pair : pageMap.entrySet()) {
            if (site.getId() == pair.getKey().getSiteEntity().getId()) {

                String snippet = getSnippet(query, pair.getKey());
                if (snippet == null) {
                    continue;
                }

                Data data = new Data()
                        .setSiteUrl(site.getSiteUrl())
                        .setSiteName(site.getSiteName())
                        .setUri(pair.getKey().getPagePath())
                        .setTitle(getTitle(pair.getKey()))
                        .setSnippet(snippet)
                        .setRelevance(pair.getValue());

                dataList.add(data);
                log.info("Добавили объект в dataList: {}", data);
            }
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

    private List<PageEntity> getPageList(LemmaEntity lemmaEntity) {
        List<IndexEntity> indexEntityList = indexRepository.findAllByLemma(lemmaEntity);

        List<PageEntity> pageList = new ArrayList<>();
        for (IndexEntity indexEntity : indexEntityList) {
            PageEntity pageEntity = pageRepository.findById(indexEntity.getPage().getId());
            pageList.add(pageEntity);
        }
        return pageList;
    }

    @SneakyThrows
    private String getTitle(PageEntity page) {
        String pageContent = pageRepository.findByPagePathAndSiteEntity(page.getPagePath(), page.getSiteEntity()).getContentPage();
        int indexOfStartTitle = pageContent.indexOf("<title>");
        int indexOfEndTitle = pageContent.indexOf("</title>");

        StringBuilder title;
        if (indexOfStartTitle == -1) {
            return "У страницы нет заголовка";
        } else {
            title = new StringBuilder(pageContent.substring(indexOfStartTitle + 7, indexOfEndTitle));
        }

        return title.toString();
    }

    @SneakyThrows
    private String getSnippet(String query, PageEntity page) {
        String pageContent = pageRepository
                .findByPagePathAndSiteEntity(page.getPagePath(),
                        page.getSiteEntity()).getContentPage().toLowerCase()
                .replaceAll("[^А-Яа-яёЁ]", " ").replaceAll("\\s+(.*?)", " ");

        int startIndex = pageContent.indexOf(query);
        if (startIndex == -1) {
            return null;
        }

        int endIndex = pageContent.indexOf(" ", startIndex);
        String currentWord = pageContent.substring(startIndex, endIndex);

        if (!currentWord.equals(query) && (pageContent.charAt(startIndex + query.length()) != ' '
                || pageContent.charAt(startIndex - 1) != ' ')) {
            return null;
        }

        StringBuilder snippet = null;
        if (startIndex < 150) {
            snippet = new StringBuilder(pageContent.substring(0, startIndex + 150));
        }
        if (pageContent.length() - startIndex < 150) {
            snippet = new StringBuilder(pageContent.substring(startIndex - 150));
        }
        if (startIndex >= 150 && pageContent.length() - startIndex >= 150) {
            snippet = new StringBuilder(pageContent.substring(startIndex - 150, startIndex + 150));
        }

        return getBoldFont(snippet, query).toString();
    }

    private StringBuilder getBoldFont(StringBuilder snippet, String query) {
        int index = snippet.indexOf(query);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(snippet.substring(0, index))
                .append("<b>" + query + "</b>")
                .append(snippet.substring(index + query.length(), snippet.length()));

        return stringBuilder;
    }
}
