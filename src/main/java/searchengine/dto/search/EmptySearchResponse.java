package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmptySearchResponse implements SearchResponse {
    private final boolean result = false;
    private final String error = "Задан пустой поисковый запрос";

    @Override
    public boolean getResult() {
        return result;
    }
}
