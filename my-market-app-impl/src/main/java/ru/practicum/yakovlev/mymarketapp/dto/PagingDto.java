package ru.practicum.yakovlev.mymarketapp.dto;

import lombok.Builder;

@Builder
public record PagingDto(
        int pageSize,
        int pageNumber,
        boolean hasPrevious,
        boolean hasNext
) {
}
