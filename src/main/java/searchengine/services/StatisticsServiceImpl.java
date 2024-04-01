package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.IndexProperties;
import searchengine.config.StatisticsProperties;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsRsDto;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final IndexProperties sites;
    private final StatisticsProperties statisticsProperties;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @SneakyThrows
    @Override
    public StatisticsRsDto getStatistics() {
        log.info("getStatistics > начал работу");

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();

            SiteEntity siteEntity = siteRepository.findBySiteUrl(site.getUrl());
            if (siteEntity != null) {
                item.setName(site.getName());
                item.setUrl(site.getUrl());

                int pages = getCountOfPage(site);
                int lemmas = getCountOfLemma(site);
                item.setPages(pages);
                item.setLemmas(lemmas);

                item.setStatus(siteRepository.findBySiteUrl(site.getUrl()).getStatusIndexing().toString());

                if (item.getStatus().equals(statisticsProperties.getStatuses().getFailed())) {
                    item.setError(statisticsProperties.getStatusMessages().getSiteUnavailable());
                } else {
                    item.setError(statisticsProperties.getStatusMessages().getOk());
                }

                LocalDateTime siteDateTime = siteRepository.findBySiteUrl(site.getUrl()).getStatusTime();
                ZonedDateTime zdt = ZonedDateTime.of(siteDateTime, ZoneId.systemDefault());
                long time = zdt.toInstant().toEpochMilli();
                item.setStatusTime(time);

                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
            } else {
                item.setName(site.getName());
                item.setUrl(site.getUrl());
                //item.setStatus(statisticsProperties.getStatuses().getWarning());
                item.setError(statisticsProperties.getStatusMessages().getWarning());
            }

            detailed.add(item);
        }

        StatisticsRsDto response = new StatisticsRsDto();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        log.info("getStatistics > завершился");
        return response;
    }

    private int getCountOfPage(Site site) {
        SiteEntity siteEntity = siteRepository.findBySiteUrl(site.getUrl());
        return pageRepository.findAllBySiteEntity(siteEntity).size();
    }
    private int getCountOfLemma(Site site) {
        SiteEntity siteEntity = siteRepository.findBySiteUrl(site.getUrl());
        return lemmaRepository.findAllBySite(siteEntity).size();
    }

}
