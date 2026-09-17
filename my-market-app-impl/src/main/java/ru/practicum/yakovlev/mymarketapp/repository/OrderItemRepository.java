package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.yakovlev.mymarketapp.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
