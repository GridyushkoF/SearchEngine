package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SuccessfulSearchResponse implements SearchResponse {
    private final List<SearchResult> data;
    private boolean result;
    private int count;

    public boolean getResult() {
        return result;
    }
}
