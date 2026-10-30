package ru.practicum.yakovlev.mymarketapp.exception;

public class PaymentException extends RuntimeException {
    private final String reason;

    public PaymentException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
