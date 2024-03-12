package searchengine.dto.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class UncorrectSearchResponse implements SearchResponse {
    private final boolean result = false;
    private final String error;
    @Override
    public boolean getResult() {
        return result;
    }
}
