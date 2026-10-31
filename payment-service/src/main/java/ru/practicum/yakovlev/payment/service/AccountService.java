package ru.practicum.yakovlev.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AccountService {
    private final AtomicReference<BigDecimal> balance;

    public AccountService(@Value("${payment.initial-balance}") BigDecimal initialBalance) {
        if (initialBalance == null || initialBalance.signum() < 0 || initialBalance.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Initial balance must be non-negative with at most two decimal places");
        }
        balance = new AtomicReference<>(initialBalance);
    }

    public Mono<BigDecimal> getBalance() {
        return Mono.fromSupplier(balance::get);
    }

    /** Processes a payment whose amount has passed API validation. */
    public Mono<BigDecimal> pay(BigDecimal amount) {
        return Mono.fromSupplier(() -> debit(amount));
    }

    private BigDecimal debit(BigDecimal amount) {
        while (true) {
            BigDecimal current = balance.get();
            BigDecimal remaining = current.subtract(amount);
            if (remaining.signum() < 0) {
                throw new PaymentRejectedException("INSUFFICIENT_FUNDS", "Insufficient funds");
            }
            if (balance.compareAndSet(current, remaining)) {
                return remaining;
            }
        }
    }
}
