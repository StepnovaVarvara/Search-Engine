package searchengine.dto.indexing;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErrorRsDto {
    private boolean result;
    private String error;
}
