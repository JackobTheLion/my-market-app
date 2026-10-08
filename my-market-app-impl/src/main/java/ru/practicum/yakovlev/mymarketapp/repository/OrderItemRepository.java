package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.practicum.yakovlev.mymarketapp.model.OrderItem;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {
    Flux<OrderItem> findAllByOrderIdOrderByIdAsc(long orderId);

    Flux<OrderItem> findAllByOrderByOrderIdAsc();
}
