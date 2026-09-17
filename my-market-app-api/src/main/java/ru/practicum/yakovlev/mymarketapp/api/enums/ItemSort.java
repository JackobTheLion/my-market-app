package ru.practicum.yakovlev.mymarketapp.api.enums;

import org.springframework.data.domain.Sort;

public enum ItemSort {
    NO(Sort.unsorted()),
    ALPHA(Sort.by("title")),
    PRICE(Sort.by("price"));

    private final Sort sort;

    ItemSort(Sort sort) {
        this.sort = sort;
    }

    public Sort getSort() {
        return sort;
    }
}
