package searchengine.dto.indexPage;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IndexingPageRsDto {
    private boolean result;
}
