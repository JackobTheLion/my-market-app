package ru.practicum.yakovlev.mymarketapp.exception;

import lombok.Getter;
import ru.practicum.yakovlev.mymarketapp.api.dto.ItemCreateRequest;

@Getter
public class ItemCreationException extends RuntimeException {
    private final ItemCreateRequest request;

    public ItemCreationException(ItemCreateRequest request, Throwable cause) {
        super("Cannot create product", cause);
        this.request = request;
    }
}
