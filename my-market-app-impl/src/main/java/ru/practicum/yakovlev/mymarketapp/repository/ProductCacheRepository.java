package ru.practicum.yakovlev.mymarketapp.repository;

import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.cache.CachedItem;
import ru.practicum.yakovlev.mymarketapp.cache.CachedItemPage;

import java.util.List;
import java.util.Map;

public interface ProductCacheRepository {
    Mono<CachedItem> findById(long id);

    Mono<Map<Long, CachedItem>> findAllById(List<Long> ids);

    Mono<Void> save(CachedItem item);

    Mono<Void> saveAll(List<CachedItem> items);

    Mono<CachedItemPage> findPage(String search, ItemSort sort, int pageNumber, int pageSize);

    Mono<Void> savePage(String search, ItemSort sort, int pageNumber, int pageSize, CachedItemPage page);

    Mono<Void> invalidatePages();
}
