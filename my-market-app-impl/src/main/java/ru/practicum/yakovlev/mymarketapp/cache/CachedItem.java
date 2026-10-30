package ru.practicum.yakovlev.mymarketapp.cache;

import ru.practicum.yakovlev.mymarketapp.model.Item;

import java.math.BigDecimal;

public record CachedItem(Long id, String title, String description, String imagePath, BigDecimal price) {

    public static CachedItem from(Item item) {
        return new CachedItem(item.getId(), item.getTitle(), item.getDescription(), item.getImagePath(), item.getPrice());
    }

    public Item toItem() {
        Item item = new Item(title, description, imagePath, price);
        item.setId(id);
        return item;
    }
}
