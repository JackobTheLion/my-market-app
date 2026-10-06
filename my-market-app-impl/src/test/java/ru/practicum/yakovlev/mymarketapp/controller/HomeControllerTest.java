package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import ru.practicum.yakovlev.mymarketapp.support.WebFluxTestSupport;

class HomeControllerTest extends WebFluxTestSupport {
    @Test
    void redirectsToCatalog() {
        client.get()
                .uri("/")
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/items");
    }
}
