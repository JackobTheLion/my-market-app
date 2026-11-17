package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.test.context.support.WithUserDetails;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.exception.EmptyCartException;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException.Reason;
import ru.practicum.yakovlev.mymarketapp.support.WebFluxTestSupport;

import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.TEST_USER_ID;
import static org.mockito.Mockito.when;

@WithUserDetails(value = "demo", userDetailsServiceBeanName = "testCustomerDetailsService")
class BuyControllerTest extends WebFluxTestSupport {

    @ParameterizedTest
    @EnumSource(Reason.class)
    void unsuccessfulPaymentRedirectsBackToCart(Reason reason) {
        when(orderService.createOrder(TEST_USER_ID)).thenReturn(Mono.error(
                new PaymentException(reason, null)));
        client.post().uri("/buy").exchange().expectStatus().isSeeOther()
                .expectHeader().location("/cart/items?paymentError=" + reason.getCode());
    }

    @Test
    void successfulPurchaseRedirectsToOrder() {
        when(orderService.createOrder(TEST_USER_ID)).thenReturn(Mono.just(42L));

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
        when(orderService.createOrder(TEST_USER_ID)).thenReturn(Mono.error(new EmptyCartException()));

        client.post()
                .uri("/buy")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }
}
