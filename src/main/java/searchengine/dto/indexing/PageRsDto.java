package searchengine.dto.indexing;

import lombok.Data;
import org.jsoup.Connection;

@Data
public class PageRsDto {
    private int statusCode;
    private String body;
    private String statusMessage;

    public PageRsDto(Connection.Response response) {
        this.statusCode = response.statusCode();
        this.body = response.body();
        this.statusMessage = response.statusMessage();
    }
}
