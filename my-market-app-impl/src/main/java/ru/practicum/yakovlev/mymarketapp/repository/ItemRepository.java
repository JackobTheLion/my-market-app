package ru.practicum.yakovlev.mymarketapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.yakovlev.mymarketapp.model.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description, Pageable pageable);
}
