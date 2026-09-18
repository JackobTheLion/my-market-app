package ru.practicum.yakovlev.mymarketapp.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrdersPageDto(
        List<OrderDto> orders,
        BigDecimal totalSum
) {
}
