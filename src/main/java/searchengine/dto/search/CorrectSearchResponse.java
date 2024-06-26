package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CorrectSearchResponse implements SearchResponse {
    private final List<SearchResult> data;
    private boolean result;
    private int count;
    @Override
    public boolean getResult() {
        return result;
    }
}
