package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.exception.EmptyCartException;
import ru.practicum.yakovlev.mymarketapp.support.WebFluxTestSupport;

import static org.mockito.Mockito.when;

class BuyControllerTest extends WebFluxTestSupport {
    @Test
    void successfulPurchaseRedirectsToOrder() {
        when(orderService.createOrder()).thenReturn(Mono.just(42L));

        client.post()
                .uri("/buy")
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .location("/orders/42?newOrder=true");
    }

    @Test
    void emptyCartReturns400() {
        when(orderService.createOrder()).thenReturn(Mono.error(new EmptyCartException()));

        client.post()
                .uri("/buy")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }
}
