package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = "item")
    List<CartItem> findAllByOrderByItemIdAsc();
}
