package ru.practicum.yakovlev.mymarketapp.service;

import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;

public interface CartService {

    Mono<CartDto> getCart();

    Mono<Void> updateItem(long itemId, CartAction action);
}
