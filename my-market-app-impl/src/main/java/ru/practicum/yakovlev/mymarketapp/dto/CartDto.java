package ru.practicum.yakovlev.mymarketapp.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
        List<ItemDto> items,
        BigDecimal total
) {
}
