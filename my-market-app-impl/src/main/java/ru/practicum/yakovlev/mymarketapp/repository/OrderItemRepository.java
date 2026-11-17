package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.practicum.yakovlev.mymarketapp.model.OrderItem;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {
    Flux<OrderItem> findAllByOrderIdOrderByIdAsc(long orderId);

    @Query("""
            SELECT oi.* FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.user_id = :userId
            ORDER BY oi.order_id, oi.id
            """)
    Flux<OrderItem> findAllByUserId(long userId);
}
