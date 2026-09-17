package ru.practicum.yakovlev.mymarketapp.dto;

import java.util.List;

public record ItemPageDto(
        List<List<ItemDto>> items,
        PagingDto paging
) {
}
