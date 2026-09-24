package ru.practicum.yakovlev.mymarketapp.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderDto(
        long id,
        List<OrderItemDto> items,
        BigDecimal totalSum
) {
}
