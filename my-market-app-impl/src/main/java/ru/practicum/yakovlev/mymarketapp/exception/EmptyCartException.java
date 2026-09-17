package ru.practicum.yakovlev.mymarketapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Cannot place an order with an empty cart");
    }
}
