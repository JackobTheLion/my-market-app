package ru.practicum.yakovlev.mymarketapp.payment;

import reactor.core.publisher.Mono;
import java.math.BigDecimal;

public interface PaymentGateway {

    Mono<BigDecimal> getBalance(long userId);

    Mono<Void> pay(long userId, BigDecimal amount);

}
