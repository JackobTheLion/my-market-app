package ru.practicum.yakovlev.mymarketapp.service;

import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;

public interface ItemService {

    Mono<ItemPageDto> getItems(String search, ItemSort sort, int pageNumber, int pageSize);

    Mono<ItemDto> getItem(long id);
}
