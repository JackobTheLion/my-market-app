package ru.practicum.yakovlev.mymarketapp.cache;

import java.util.List;

public record CachedItemPage(List<CachedItem> items, long total) {
    public CachedItemPage {
        items = List.copyOf(items);
        if (total < items.size()) {
            throw new IllegalArgumentException("Invalid cached product count");
        }
    }
}
