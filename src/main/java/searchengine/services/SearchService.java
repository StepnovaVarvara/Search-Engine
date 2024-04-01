package searchengine.services;

import searchengine.dto.search.SearchRsDto;

public interface SearchService {
    SearchRsDto search(String query, String site, int offset, int limit);
}
