package ru.practicum.yakovlev.mymarketapp.service;

import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.dto.ItemCreateRequest;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;
import ru.practicum.yakovlev.mymarketapp.model.Item;

import java.util.List;
import java.util.Map;

public interface ItemService {

    Mono<ItemPageDto> getItems(Long userId, String search, ItemSort sort, int pageNumber, int pageSize);

    Mono<ItemDto> getItem(Long userId, long id);

    Mono<Map<Long, Item>> getItemsByIds(List<Long> ids);

    Mono<Long> createItem(ItemCreateRequest request);

}
