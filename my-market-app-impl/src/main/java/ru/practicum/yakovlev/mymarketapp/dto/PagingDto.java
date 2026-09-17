package ru.practicum.yakovlev.mymarketapp.dto;

public record PagingDto(
        int pageSize,
        int pageNumber,
        boolean hasPrevious,
        boolean hasNext
) {
}
