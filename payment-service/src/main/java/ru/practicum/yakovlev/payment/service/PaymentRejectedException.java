package ru.practicum.yakovlev.payment.service;

import lombok.Getter;

@Getter
public class PaymentRejectedException extends RuntimeException {
    private final String code;

    public PaymentRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }
}
