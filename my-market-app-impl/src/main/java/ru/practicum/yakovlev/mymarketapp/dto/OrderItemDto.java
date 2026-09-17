package ru.practicum.yakovlev.mymarketapp.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        long id,
        String title,
        BigDecimal price,
        int count
) {
}
