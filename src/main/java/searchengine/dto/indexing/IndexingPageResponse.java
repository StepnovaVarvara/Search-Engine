package searchengine.dto.indexing;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IndexingPageResponse {
    private boolean result;
}
