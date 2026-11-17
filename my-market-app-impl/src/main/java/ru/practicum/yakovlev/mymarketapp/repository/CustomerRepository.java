package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.model.Customer;

public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {

    Mono<Customer> findByUsername(String username);
}
