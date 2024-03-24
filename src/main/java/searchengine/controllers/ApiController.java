package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexPage.IndexingPageResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final StopIndexingService stopIndexingService;
    private final IndexingPageService indexingPageService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(stopIndexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingPageResponse> indexPage(@RequestParam String url) {
        log.info("indexPage > url: {}", url);
        return ResponseEntity.ok(indexingPageService.startIndexingPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam(value = "site", required = false)  String site,
                                                 @RequestParam int offset, @RequestParam int limit) {
        log.info("search > site: {}", site);
        log.info("search > query: {}", query);
        log.info("search > offset: {}", offset);
        log.info("search > limit: {}", limit);
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}
