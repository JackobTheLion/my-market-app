package ru.practicum.yakovlev.mymarketapp.payment;

import reactor.core.publisher.Mono;
import java.math.BigDecimal;

public interface PaymentGateway {
    Mono<BigDecimal> getBalance();
    Mono<Void> pay(BigDecimal amount);
}
