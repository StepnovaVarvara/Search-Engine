package searchengine.dto.indexing;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErrorIndexingResponse {
    private boolean result;
    private String error;
}
