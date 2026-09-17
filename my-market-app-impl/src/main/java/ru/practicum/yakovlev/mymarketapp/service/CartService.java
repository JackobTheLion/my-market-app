package ru.practicum.yakovlev.mymarketapp.service;

import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;

public interface CartService {

    CartDto getCart();

    void updateItem(long itemId, CartAction action);
}
