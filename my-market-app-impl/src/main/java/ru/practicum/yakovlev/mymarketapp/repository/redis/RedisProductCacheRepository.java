package ru.practicum.yakovlev.mymarketapp.repository.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.cache.CachedItem;
import ru.practicum.yakovlev.mymarketapp.cache.CachedItemPage;
import ru.practicum.yakovlev.mymarketapp.repository.ProductCacheRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class RedisProductCacheRepository implements ProductCacheRepository {

    private static final String PREFIX = "market:products:v1:";

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;
    private final Duration timeout;

    public RedisProductCacheRepository(
            ReactiveStringRedisTemplate redis,
            ObjectMapper mapper,
            @Value("${application.cache.product-ttl}") Duration ttl,
            @Value("${application.cache.timeout}") Duration timeout
    ) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttl = ttl;
        this.timeout = timeout;
    }

    @Override
    public Mono<CachedItem> findById(long id) {
        return read(itemKey(id), CachedItem.class).filter(item -> item.id() == id);
    }

    @Override
    public Mono<Map<Long, CachedItem>> findAllById(List<Long> ids) {
        if (ids.isEmpty()) {
            return Mono.just(Map.of());
        }
        return redis.opsForValue().multiGet(ids.stream().map(this::itemKey).toList())
                .timeout(timeout)
                .map(values -> deserializeItems(ids, values));
    }

    @Override
    public Mono<Void> save(CachedItem item) {
        return write(itemKey(item.id()), item);
    }

    @Override
    public Mono<Void> saveAll(List<CachedItem> items) {
        return Flux.fromIterable(items)
                .flatMap(this::save, 8).then()
                .timeout(timeout);
    }

    @Override
    public Mono<CachedItemPage> findPage(String search, ItemSort sort, int pageNumber, int pageSize) {
        return read(pageKey(search, sort, pageNumber, pageSize), CachedItemPage.class);
    }

    @Override
    public Mono<Void> savePage(String search, ItemSort sort, int pageNumber, int pageSize, CachedItemPage page) {
        return write(pageKey(search, sort, pageNumber, pageSize), page);
    }

    @Override
    public Mono<Void> invalidatePages() {
        Flux<String> keys = redis.scan(ScanOptions.scanOptions().match(PREFIX + "page:*").count(100).build());
        return redis.delete(keys).then().timeout(timeout);
    }

    private Map<Long, CachedItem> deserializeItems(List<Long> ids, List<String> values) {
        Map<Long, CachedItem> items = new HashMap<>();
        for (int index = 0; index < ids.size(); index++) {
            deserializeItem(ids.get(index), values.get(index))
                    .ifPresent(item -> items.put(item.id(), item));
        }
        return Map.copyOf(items);
    }

    private Optional<CachedItem> deserializeItem(Long id, String json) {
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(mapper.readValue(json, CachedItem.class))
                    .filter(item -> id.equals(item.id()));
        } catch (JsonProcessingException error) {
            log.debug("Invalid cached product {}", id, error);
            return Optional.empty();
        }
    }

    private <T> Mono<T> read(String key, Class<T> type) {
        return redis.opsForValue().get(key).timeout(timeout)
                .flatMap(json -> Mono.fromCallable(() -> mapper.readValue(json, type))
                        .onErrorResume(JsonProcessingException.class, error -> {
                            log.debug("Invalid cache entry {}", key, error);
                            return Mono.empty();
                        }));
    }

    private Mono<Void> write(String key, Object value) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(value))
                .flatMap(json -> redis.opsForValue().set(key, json, ttl)).timeout(timeout).then();
    }

    private String itemKey(long id) {
        return PREFIX + "item:" + id;
    }

    private String pageKey(String search, ItemSort sort, int pageNumber, int pageSize) {
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(search.getBytes(StandardCharsets.UTF_8)));
            return PREFIX + "page:" + hash + ":" + sort + ":" + pageNumber + ":" + pageSize;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
