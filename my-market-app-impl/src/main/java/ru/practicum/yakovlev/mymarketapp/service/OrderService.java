package ru.practicum.yakovlev.mymarketapp.service;

import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.dto.OrdersPageDto;

public interface OrderService {

    Mono<OrdersPageDto> getOrders(long userId);

    Mono<OrderDto> getOrder(long userId, long id);

    Mono<Long> createOrder(long userId);
}
