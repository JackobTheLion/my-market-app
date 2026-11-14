package ru.practicum.yakovlev.mymarketapp.service;

import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;

public interface CartService {

    Mono<CartDto> getCart(long userId);

    Mono<Void> updateItem(long userId, long itemId, CartAction action);
}
