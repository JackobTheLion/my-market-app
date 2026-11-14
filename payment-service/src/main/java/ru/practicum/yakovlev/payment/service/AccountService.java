package ru.practicum.yakovlev.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AccountService {
    private final ConcurrentMap<Long, BigDecimal> balances = new ConcurrentHashMap<>();
    private final BigDecimal initialBalance;

    public AccountService(@Value("${payment.initial-balance}") BigDecimal initialBalance) {
        if (initialBalance == null || initialBalance.signum() < 0 || initialBalance.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Initial balance must be non-negative with at most two decimal places");
        }
        this.initialBalance = initialBalance;
    }

    public Mono<BigDecimal> getBalance(long userId) {
        return Mono.fromSupplier(() -> balances.getOrDefault(userId, initialBalance));
    }

    /** Processes a payment whose amount has passed API validation. */
    public Mono<BigDecimal> pay(long userId, BigDecimal amount) {
        return Mono.fromSupplier(() -> balances.compute(userId, (id, balance) -> {
            BigDecimal current = balance == null ? initialBalance : balance;
            BigDecimal remaining = current.subtract(amount);
            if (remaining.signum() < 0) {
                throw new PaymentRejectedException("INSUFFICIENT_FUNDS", "Insufficient funds");
            }
            return remaining;
        }));
    }
}
