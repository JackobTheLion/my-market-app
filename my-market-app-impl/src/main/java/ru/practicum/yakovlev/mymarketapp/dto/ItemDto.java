package ru.practicum.yakovlev.mymarketapp.dto;

import java.math.BigDecimal;

public record ItemDto(
        long id,
        String title,
        String description,
        String imgPath,
        BigDecimal price,
        int count
) {

    private static final ItemDto DEFAULT_ITEM = new ItemDto(-1, "", "", "", BigDecimal.ZERO, 0);

    public static ItemDto defaultItem() {
        return DEFAULT_ITEM;
    }

}
