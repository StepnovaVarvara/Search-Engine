package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexPage.IndexingPageRsDto;
import searchengine.dto.indexing.IndexingRsDto;
import searchengine.dto.search.SearchRsDto;
import searchengine.dto.statistics.StatisticsRsDto;
import searchengine.services.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final StopIndexingService stopIndexingService;
    private final IndexingPageService indexingPageService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public StatisticsRsDto statistics() {
        return statisticsService.getStatistics();
    }
    @SneakyThrows
    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingRsDto> startIndexing() {
        CompletableFuture<Void> async = CompletableFuture.runAsync(indexingService::startIndexing);
        return ResponseEntity.ok(new IndexingRsDto().setResult(true));
    }

    @GetMapping("/stopIndexing")
    public IndexingRsDto stopIndexing() {
        return stopIndexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public IndexingPageRsDto indexPage(@RequestParam String url) {
        log.info("indexPage > url: {}", url);
        return indexingPageService.startIndexingPage(url);
    }

    @GetMapping("/search")
    public SearchRsDto search(@RequestParam String query,
                                              @RequestParam(value = "site", required = false)  String site,
                                              @RequestParam int offset, @RequestParam int limit) {
        log.info("search > site: {}", site);
        log.info("search > query: {}", query);
        log.info("search > offset: {}", offset);
        log.info("search > limit: {}", limit);
        return searchService.search(query, site, offset, limit);
    }
}
