package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SearchResult {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;


}
