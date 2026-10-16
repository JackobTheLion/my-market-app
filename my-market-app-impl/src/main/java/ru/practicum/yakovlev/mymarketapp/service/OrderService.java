package ru.practicum.yakovlev.mymarketapp.service;

import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.dto.OrdersPageDto;

public interface OrderService {

    Mono<OrdersPageDto> getOrders();

    Mono<OrderDto> getOrder(long id);

    Mono<Long> createOrder();
}
