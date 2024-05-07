package searchengine.dto.search;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class Snippet {
    private final List<String> lemmasFromQuery;
    private final int beginIndexOnPage;
    private final int endIndexOnPage;
}
