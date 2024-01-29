package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingPageResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingPageService;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;
import searchengine.services.StopIndexingService;

@RestControllerAdvice
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final StopIndexingService stopIndexingService;
    private final IndexingPageService indexingPageService;

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
        return ResponseEntity.ok(indexingPageService.startIndexingPage(url));
    }
}
