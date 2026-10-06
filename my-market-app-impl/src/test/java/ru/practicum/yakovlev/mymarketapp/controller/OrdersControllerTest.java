package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.dto.OrdersPageDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.support.WebFluxTestSupport;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrdersControllerTest extends WebFluxTestSupport {
    @Test
    void rendersHistory() {
        when(orderService.getOrders()).thenReturn(Mono.just(new OrdersPageDto(List.of(new OrderDto(42, List.of(), new BigDecimal("25.00"))), new BigDecimal("25.00"))));
        client.get()
                .uri("/orders")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("25.00", "/orders/42"));
    }

    @Test
    void rendersNewOrder() {
        when(orderService.getOrder(42)).thenReturn(Mono.just(new OrderDto(42, List.of(), new BigDecimal("25.00"))));
        client.get()
                .uri("/orders/42")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("25.00"));
    }

    @Test
    void missingOrderReturns404() {
        when(orderService.getOrder(99)).thenReturn(Mono.error(new NotFoundException("missing")));
        client.get()
                .uri("/orders/99")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void nonPositiveIdReturns400() {
        client.get()
                .uri("/orders/0")
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(orderService);
    }
}
