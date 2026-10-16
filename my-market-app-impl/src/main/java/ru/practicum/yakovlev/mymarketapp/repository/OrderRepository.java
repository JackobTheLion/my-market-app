package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.practicum.yakovlev.mymarketapp.model.Order;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Flux<Order> findAllByOrderByCreatedAtDescIdDesc();

}
