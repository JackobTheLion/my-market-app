package ru.practicum.yakovlev.mymarketapp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.dto.ItemCreateRequest;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.cache.CachedItem;
import ru.practicum.yakovlev.mymarketapp.cache.CachedItemPage;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;
import ru.practicum.yakovlev.mymarketapp.dto.PagingDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.ItemMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ProductCacheRepository;
import ru.practicum.yakovlev.mymarketapp.service.ImageService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final ProductCacheRepository cache;
    private final CartItemRepository cartItemRepository;
    private final ItemMapper itemMapper;
    private final ImageService imageService;

    @Override
    public Mono<Long> createItem(ItemCreateRequest request) {
        return Mono.usingWhen(
                        Mono.defer(() -> imageService.saveImage(request.getImage())),
                        path -> itemRepository.save(new Item(request.getTitle(), request.getDescription(), path, request.getPrice())),
                        path -> Mono.empty(),
                        (path, error) -> deleteUploadedImage(path),
                        this::deleteUploadedImage)
                .delayUntil(item -> writeCache(cache::invalidatePages))
                .map(Item::getId);
    }

    private Mono<Void> deleteUploadedImage(String path) {
        return Mono.defer(() -> imageService.deleteImage(path)).onErrorResume(error -> {
            log.warn("Cannot remove image after failed product creation: {}", path, error);
            return Mono.empty();
        });
    }

    @Override
    public Mono<ItemPageDto> getItems(String search, ItemSort sort, int pageNumber, int pageSize) {
        String query = search == null ? "" : search.strip();
        return readCache(() -> cache.findPage(query, sort, pageNumber, pageSize))
                .switchIfEmpty(Mono.defer(() -> loadPage(query, sort, pageNumber, pageSize)
                        .flatMap(page -> writeCache(() -> cache.savePage(query, sort, pageNumber, pageSize, page))
                                .thenReturn(page))))
                .flatMap(page -> {
                    List<Item> items = page.items().stream().map(CachedItem::toItem).toList();
                    return getCartCounts(items)
                            .map(counts -> toPageDto(items, page.total(), counts, pageNumber, pageSize));
                });
    }

    @Override
    public Mono<ItemDto> getItem(long id) {
        return readCache(() -> cache.findById(id)).map(CachedItem::toItem)
                .switchIfEmpty(Mono.defer(() -> itemRepository.findById(id)
                        .flatMap(item -> writeCache(() -> cache.save(CachedItem.from(item))).thenReturn(item))))
                .switchIfEmpty(Mono.error(new NotFoundException("Item not found: " + id)))
                .flatMap(item -> cartItemRepository.findById(id)
                        .map(CartItem::getQuantity).defaultIfEmpty(0)
                        .map(count -> itemMapper.toDto(item, count)));
    }

    @Override
    public Mono<Map<Long, Item>> getItemsByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Mono.just(Map.of());
        }
        return readCache(() -> cache.findAllById(ids))
                .map(items -> new CacheLookup(items, true))
                .defaultIfEmpty(new CacheLookup(Map.of(), false))
                .flatMap(lookup -> loadMissingItems(ids, lookup));
    }

    private Mono<Map<Long, Item>> loadMissingItems(List<Long> ids, CacheLookup lookup) {
        Map<Long, Item> products = new HashMap<>();
        lookup.items().forEach((id, item) -> products.put(id, item.toItem()));
        List<Long> missing = ids.stream().distinct().filter(id -> !products.containsKey(id)).toList();
        if (missing.isEmpty()) {
            return Mono.just(Map.copyOf(products));
        }
        return itemRepository.findAllById(missing).collectList().flatMap(loaded -> {
            loaded.forEach(item -> products.put(item.getId(), item));
            for (Long id : missing) {
                if (!products.containsKey(id)) {
                    return Mono.error(new NotFoundException("Item not found: " + id));
                }
            }
            Mono<Void> populate = lookup.available()
                    ? writeCache(() -> cache.saveAll(loaded.stream().map(CachedItem::from).toList()))
                    : Mono.empty();
            return populate.thenReturn(Map.copyOf(products));
        });
    }

    private Mono<CachedItemPage> loadPage(String query, ItemSort sort, int pageNumber, int pageSize) {
        PageRequest request = PageRequest.of(pageNumber - 1, pageSize, sort.getSort());
        return Mono.zip(
                itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, request)
                        .map(CachedItem::from).collectList(),
                itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query),
                CachedItemPage::new);
    }

    private <T> Mono<T> readCache(Supplier<Mono<T>> operation) {
        return Mono.defer(operation).onErrorResume(error -> {
            log.debug("Product cache read failed; loading from database", error);
            return Mono.empty();
        });
    }

    private Mono<Void> writeCache(Supplier<Mono<Void>> operation) {
        return Mono.defer(operation).onErrorResume(error -> {
            log.debug("Product cache write failed", error);
            return Mono.empty();
        });
    }

    private Mono<Map<Long, Integer>> getCartCounts(List<Item> items) {
        if (items.isEmpty()) {
            return Mono.just(Map.of());
        }
        return cartItemRepository.findAllById(items.stream().map(Item::getId).toList())
                .collectMap(CartItem::getItemId, CartItem::getQuantity);
    }

    private ItemPageDto toPageDto(List<Item> items, long total, Map<Long, Integer> counts,
                                  int pageNumber, int pageSize) {
        List<ItemDto> dtos = items.stream()
                .map(item -> itemMapper.toDto(item, counts.getOrDefault(item.getId(), 0)))
                .toList();
        PagingDto paging = PagingDto.builder()
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .hasPrevious(pageNumber > 1)
                .hasNext(total > (long) pageNumber * pageSize)
                .build();
        return new ItemPageDto(dtos, paging);
    }

    private record CacheLookup(Map<Long, CachedItem> items, boolean available) {
    }
}
