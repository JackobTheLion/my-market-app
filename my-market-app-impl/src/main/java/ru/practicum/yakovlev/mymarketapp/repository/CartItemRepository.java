package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;

import java.util.List;

public interface CartItemRepository extends Repository<CartItem, Long>, CartItemRepositoryCustom {

    @Query("SELECT item_id, quantity FROM cart_items WHERE user_id = :userId ORDER BY item_id")
    Flux<CartItem> findAllByUserId(long userId);

    @Query("SELECT item_id, quantity FROM cart_items WHERE user_id = :userId AND item_id IN (:itemIds)")
    Flux<CartItem> findAllByUserIdAndItemIds(long userId, List<Long> itemIds);

    @Query("SELECT item_id, quantity FROM cart_items WHERE user_id = :userId AND item_id = :itemId")
    Mono<CartItem> findItem(long userId, long itemId);

    @Query("SELECT item_id, quantity FROM cart_items WHERE user_id = :userId AND item_id = :itemId FOR UPDATE")
    Mono<CartItem> findItemForUpdate(long userId, long itemId);

    @Modifying
    @Query("DELETE FROM cart_items WHERE user_id = :userId AND item_id IN (:itemIds)")
    Mono<Integer> deletePurchasedItems(long userId, List<Long> itemIds);

    @Modifying
    @Query("INSERT INTO cart_items (user_id, item_id, quantity) VALUES (:userId, :itemId, 1) "
            + "ON CONFLICT (user_id, item_id) DO UPDATE SET quantity = cart_items.quantity + 1")
    Mono<Integer> increment(long userId, long itemId);

    @Modifying
    @Query("DELETE FROM cart_items WHERE user_id = :userId AND item_id = :itemId")
    Mono<Void> deleteItem(long userId, long itemId);

    @Modifying
    @Query("DELETE FROM cart_items WHERE user_id = :userId AND item_id = :itemId AND quantity = 1")
    Mono<Integer> deleteLast(long userId, long itemId);

    @Modifying
    @Query("UPDATE cart_items SET quantity = quantity - 1 WHERE user_id = :userId AND item_id = :itemId AND quantity > 1")
    Mono<Integer> decrement(long userId, long itemId);

    @Query("SELECT COUNT(*) FROM cart_items WHERE user_id = :userId")
    Mono<Long> countByUserId(long userId);
}
