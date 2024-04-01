package searchengine.dto.search;

import lombok.experimental.Accessors;

import java.util.List;

@lombok.Data
@Accessors(chain = true)
public class SearchRsDto {
    private boolean result;
    private int count;
    private List<Data> data;
}
