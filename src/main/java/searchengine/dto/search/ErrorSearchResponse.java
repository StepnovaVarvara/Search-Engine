package searchengine.dto.search;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErrorSearchResponse {
    private boolean result;
    private String error;
}
