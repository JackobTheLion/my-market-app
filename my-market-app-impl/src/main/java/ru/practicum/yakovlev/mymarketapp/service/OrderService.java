package ru.practicum.yakovlev.mymarketapp.service;

import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.dto.OrdersPageDto;

public interface OrderService {

    OrdersPageDto getOrders();

    OrderDto getOrder(long id);

    long createOrder();
}
