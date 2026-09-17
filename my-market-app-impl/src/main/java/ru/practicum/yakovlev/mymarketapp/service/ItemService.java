package ru.practicum.yakovlev.mymarketapp.service;

import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;

public interface ItemService {

    ItemPageDto getItems(String search, ItemSort sort, int pageNumber, int pageSize);

    ItemDto getItem(long id);
}
