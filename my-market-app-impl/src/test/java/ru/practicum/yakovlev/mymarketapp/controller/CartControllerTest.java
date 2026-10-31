package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException;
import ru.practicum.yakovlev.mymarketapp.support.WebFluxTestSupport;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CartControllerTest extends WebFluxTestSupport {
    @Test
    void enoughBalanceEnablesPurchase() {
        mockNonEmptyCart();
        when(paymentGateway.getBalance()).thenReturn(Mono.just(new BigDecimal("100")));
        client.get().uri("/cart/items").exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("Купить")
                        .doesNotContain("disabled=\"disabled\"", "Заказ не оформлен"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"insufficient-funds", "rejected", "unavailable"})
    void failedPurchaseShowsPaymentError(String reason) {
        mockNonEmptyCart();
        when(paymentGateway.getBalance()).thenReturn(Mono.just(new BigDecimal("100")));

        client.get().uri("/cart/items?paymentError={reason}", reason)
                .exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("Заказ не оформлен"));
    }

    @Test
    void insufficientBalanceDisablesPurchaseWithMessage() {
        mockNonEmptyCart();
        when(paymentGateway.getBalance()).thenReturn(Mono.just(new BigDecimal("1")));
        client.get().uri("/cart/items").exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("disabled=\"disabled\"", "Недостаточно средств"));
    }

    @Test
    void unavailablePaymentServiceDisablesPurchaseWithMessage() {
        mockNonEmptyCart();
        when(paymentGateway.getBalance()).thenReturn(Mono.error(new PaymentException(
                "unavailable", "Сервис платежей недоступен", null)));
        client.get().uri("/cart/items").exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("disabled=\"disabled\"", "Сервис платежей недоступен"));
    }

    private void mockNonEmptyCart() {
        ItemDto item = new ItemDto(
                1L, "Coffee", "Description", "images/default-image.svg", new BigDecimal("12.50"), 2);
        when(cartService.getCart()).thenReturn(Mono.just(new CartDto(List.of(item), new BigDecimal("25.00"))));
    }

    @Test
    void emptyCartHidesPurchaseButton() {
        when(cartService.getCart()).thenReturn(Mono.just(new CartDto(List.of(), BigDecimal.ZERO)));

        client.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).doesNotContain("action=\"/buy\"", "Заказ не оформлен"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", ""})
    void invalidActionReturns400(String action) {
        client.post()
                .uri("/cart/items?id=1&action={action}", action)
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService);
    }

    @Test
    void queryParametersDetermineActionWhenBodyContainsOtherValues() {
        when(cartService.updateItem(1, CartAction.PLUS)).thenReturn(Mono.empty());
        client.post()
                .uri("/cart/items?id=1&action=PLUS")
                .body(BodyInserters.fromFormData("id", "99")
                        .with("action", "MINUS"))
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .location("/cart/items");
        verify(cartService)
                .updateItem(1, CartAction.PLUS);
        verifyNoMoreInteractions(cartService);
    }

    @ParameterizedTest
    @EnumSource(CartAction.class)
    void successfulUpdateRedirectsToCart(CartAction action) {
        when(cartService.updateItem(1, action)).thenReturn(Mono.empty());

        client.post()
                .uri("/cart/items?id=1&action={action}", action)
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .location("/cart/items");

        verify(cartService).updateItem(1, action);
        verifyNoMoreInteractions(cartService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "abc"})
    void invalidIdReturns400(String id) {
        client.post()
                .uri("/cart/items?id={id}&action=PLUS", id)
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService);
    }

    @Test
    void missingIdReturns400() {
        client.post()
                .uri("/cart/items?action=PLUS")
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService);
    }

    @Test
    void missingActionReturns400() {
        client.post()
                .uri("/cart/items?id=1")
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService);
    }

    @Test
    void formBodyDoesNotReplaceRequiredQueryParameters() {
        client.post()
                .uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService);
    }
}
