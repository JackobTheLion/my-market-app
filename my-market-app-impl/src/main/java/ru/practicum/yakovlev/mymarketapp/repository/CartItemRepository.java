package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;

import java.util.List;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long>, CartItemRepositoryCustom {

    @Modifying
    @Query("DELETE FROM cart_items WHERE item_id IN (:itemIds)")
    Mono<Integer> deletePurchasedItems(List<Long> itemIds);

    @Modifying
    @Query("INSERT INTO cart_items (item_id, quantity) VALUES (:itemId, 1) "
            + "ON CONFLICT (item_id) DO UPDATE SET quantity = cart_items.quantity + 1")
    Mono<Integer> increment(long itemId);

    @Modifying
    @Query("DELETE FROM cart_items WHERE item_id = :itemId AND quantity = 1")
    Mono<Integer> deleteLast(long itemId);

    @Modifying
    @Query("UPDATE cart_items SET quantity = quantity - 1 WHERE item_id = :itemId AND quantity > 1")
    Mono<Integer> decrement(long itemId);
}
