package searchengine.dto.search;

import lombok.experimental.Accessors;

@lombok.Data
@Accessors(chain = true)
public class Data implements Comparable<Data> {
    private String siteUrl;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

    @Override
    public int compareTo(Data o) {
        return  (int) ((int) o.getRelevance() - this.getRelevance());
    }
}
