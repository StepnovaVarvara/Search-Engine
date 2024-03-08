package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Messages {
    private String stop;
    private String stopError;
    private String startError;
    private String indexPageError;
    private String searchError;
    private String siteNotFound;
    private String sitesNotIndexing;
    private String offsetError;
}
