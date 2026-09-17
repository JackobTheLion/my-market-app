package ru.practicum.yakovlev.mymarketapp.service;

import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;

import java.util.List;

public interface OrderService {

    List<OrderDto> getOrders();

    OrderDto getOrder(long id);

    long createOrder();
}
