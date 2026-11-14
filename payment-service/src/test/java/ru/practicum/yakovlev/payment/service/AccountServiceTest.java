package ru.practicum.yakovlev.payment.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountServiceTest {
    @Test
    void concurrentPaymentsCannotOverdrawOrAffectAnotherAccount() {
        AccountService accounts = new AccountService(new BigDecimal("10.00"));

        StepVerifier.create(Flux.range(0, 30)
                        .flatMap(attempt -> accounts.pay(1, BigDecimal.ONE)
                                .subscribeOn(Schedulers.parallel())
                                .map(balance -> true)
                                .onErrorReturn(PaymentRejectedException.class, false))
                        .filter(Boolean::booleanValue)
                        .count())
                .expectNext(10L)
                .verifyComplete();

        assertThat(accounts.getBalance(1).block()).isEqualByComparingTo("0.00");
        assertThat(accounts.getBalance(2).block()).isEqualByComparingTo("10.00");
    }
}
