package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.exception.EmptyCartException;
import ru.practicum.yakovlev.mymarketapp.support.WebFluxTestSupport;

import static org.mockito.Mockito.when;

class BuyControllerTest extends WebFluxTestSupport {
    @ParameterizedTest
    @ValueSource(strings = {"insufficient-funds", "unavailable"})
    void unsuccessfulPaymentRedirectsBackToCart(String reason) {
        when(orderService.createOrder()).thenReturn(Mono.error(
                new PaymentException(reason, "Payment failed", null)));
        client.post().uri("/buy").exchange().expectStatus().isSeeOther()
                .expectHeader().location("/cart/items?paymentError=" + reason);
    }

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
