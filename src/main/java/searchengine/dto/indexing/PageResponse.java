package searchengine.dto.indexing;

import lombok.Data;
import org.jsoup.Connection;

@Data
public class PageResponse {
    private int statusCode;
    private String body;
    private String statusMessage;

    public PageResponse(Connection.Response response) {
        this.statusCode = response.statusCode();
        this.body = response.body();
        this.statusMessage = response.statusMessage();
    }
}
