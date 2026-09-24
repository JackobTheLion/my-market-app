package ru.practicum.yakovlev.mymarketapp.support;

import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.model.Item;

import java.math.BigDecimal;

public final class TestFixtures {
    private TestFixtures() {
    }

    public static Item item(String title, String price) {
        return new Item(title, "Description of " + title, "demo/photo.jpg", new BigDecimal(price));
    }

    public static Item item(long id, String title, String price) {
        Item item = item(title, price);
        item.setId(id);
        return item;
    }

    public static ItemDto itemDto(long id, int count) {
        return new ItemDto(id, "Coffee", "Arabica", "images/demo/coffee.jpg", new BigDecimal("12.50"), count);
    }
}
