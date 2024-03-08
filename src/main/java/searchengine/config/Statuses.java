package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Statuses {
    private String indexing;
    private String indexed;
    private String failed;
    private String warning;
}
