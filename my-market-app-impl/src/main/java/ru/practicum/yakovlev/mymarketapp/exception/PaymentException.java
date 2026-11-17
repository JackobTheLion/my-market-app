package ru.practicum.yakovlev.mymarketapp.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PaymentException extends RuntimeException {
    private final Reason reason;

    public PaymentException(Reason reason, Throwable cause) {
        super(reason.getMessage(), cause);
        this.reason = reason;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Reason {
        INSUFFICIENT_FUNDS("insufficient-funds", "Недостаточно средств для оплаты заказа"),
        REJECTED("rejected", "Платёж отклонён"),
        UNAVAILABLE("unavailable", "Сервис платежей недоступен. Попробуйте позже");

        private static final Map<String, Reason> reasonMap = init();

        private final String code;
        private final String message;

        public static Reason fromCode(String code) {
            return reasonMap.getOrDefault(code, REJECTED);
        }

        private static Map<String, Reason> init() {
            Map<String, Reason> result = new HashMap<>();
            for (Reason reason : values()) {
                result.put(reason.getCode(), reason);
            }
            return Map.copyOf(result);
        }
    }
}
