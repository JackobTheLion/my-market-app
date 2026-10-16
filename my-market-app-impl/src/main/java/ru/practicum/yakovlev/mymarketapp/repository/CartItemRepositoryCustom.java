package ru.practicum.yakovlev.mymarketapp.repository;

import reactor.core.publisher.Flux;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;

public interface CartItemRepositoryCustom {
    Flux<CartItem> findAllWithItems();

    Flux<CartItem> findAllWithItemsForUpdate();
}
