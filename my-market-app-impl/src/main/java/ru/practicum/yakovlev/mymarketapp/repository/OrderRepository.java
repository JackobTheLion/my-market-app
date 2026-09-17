package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import ru.practicum.yakovlev.mymarketapp.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    List<Order> findAllByOrderByCreatedAtDescIdDesc();

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Order> findById(Long id);
}
