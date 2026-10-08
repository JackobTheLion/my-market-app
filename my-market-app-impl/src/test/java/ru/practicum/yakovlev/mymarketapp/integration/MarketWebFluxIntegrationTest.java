package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.HtmlUtils;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderRepository;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class MarketWebFluxIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private WebTestClient client;
    @Autowired
    private ItemRepository items;
    @Autowired
    private CartItemRepository cart;
    @Autowired
    private OrderRepository orders;
    @Autowired
    private OrderService orderService;

    @Test
    void catalogCartPurchaseAndHistoryWorkOverHttp() {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        String catalogHtml = client.get()
                .uri("/items?search=coffee")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("Coffee"))
                .returnResult()
                .getResponseBody();
        List<String> buttonUrls = Pattern.compile("formaction=\"([^\"]+)\"")
                .matcher(catalogHtml)
                .results()
                .map(match -> HtmlUtils.htmlUnescape(match.group(1)))
                .toList();
        URI plusUrl = URI.create(buttonUrls.stream()
                .filter(url -> url.endsWith("&action=PLUS"))
                .findFirst()
                .orElseThrow());
        URI minusUrl = URI.create(buttonUrls.stream()
                .filter(url -> url.endsWith("&action=MINUS"))
                .findFirst()
                .orElseThrow());
        client.post()
                .uri(plusUrl)
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .location("/items?search=coffee&sort=NO&pageNumber=1&pageSize=10");
        StepVerifier.create(cart.findById(id))
                .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(1))
                .verifyComplete();
        client.post()
                .uri(minusUrl)
                .exchange()
                .expectStatus()
                .isSeeOther();
        StepVerifier.create(cart.count())
                .expectNext(0L)
                .verifyComplete();
        client.post()
                .uri(plusUrl)
                .exchange()
                .expectStatus()
                .isSeeOther();
        client.post()
                .uri("/cart/items?id={id}&action=PLUS", id)
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .location("/cart/items");
        client.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("25.00"));
        StepVerifier.create(cart.findById(id))
                .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(2))
                .verifyComplete();
        FluxExchangeResult<Void> response = client.post()
                .uri("/buy")
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .returnResult(Void.class);
        OrderDto order = orderService.getOrders()
                .block()
                .orders()
                .getFirst();
        String location = "/orders/" + order.id() + "?newOrder=true";
        assertThat(response.getResponseHeaders().getFirst("Location")).isEqualTo(location);
        assertThat(order.totalSum()).isEqualByComparingTo("25.00");
        StepVerifier.create(cart.count())
                .expectNext(0L)
                .verifyComplete();
        client.get()
                .uri(location)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Coffee", "25.00"));
        client.get()
                .uri("/orders")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Coffee", "25.00", "/orders/" + order.id()));
    }

    @Test
    void renderedCartButtonsChangeQuantityAndDeleteItem() {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        client.post()
                .uri("/items?id={id}&action=PLUS", id)
                .exchange()
                .expectStatus()
                .isSeeOther();
        client.post()
                .uri("/items?id={id}&action=PLUS", id)
                .exchange()
                .expectStatus()
                .isSeeOther();
        String cartHtml = client.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        List<String> buttonUrls = Pattern.compile("formaction=\"([^\"]+)\"")
                .matcher(cartHtml)
                .results()
                .map(match -> HtmlUtils.htmlUnescape(match.group(1)))
                .toList();
        URI minusUrl = URI.create(buttonUrls.stream()
                .filter(url -> url.endsWith("&action=MINUS"))
                .findFirst()
                .orElseThrow());
        URI plusUrl = URI.create(buttonUrls.stream()
                .filter(url -> url.endsWith("&action=PLUS"))
                .findFirst()
                .orElseThrow());
        URI deleteUrl = URI.create(buttonUrls.stream()
                .filter(url -> url.endsWith("&action=DELETE"))
                .findFirst()
                .orElseThrow());

        List<URI> actions = List.of(minusUrl, plusUrl, deleteUrl);
        List<Integer> quantities = List.of(1, 2, 0);
        for (int index = 0; index < actions.size(); index++) {
            int quantity = quantities.get(index);
            client.post()
                    .uri(actions.get(index))
                    .exchange()
                    .expectStatus()
                    .isSeeOther()
                    .expectHeader()
                    .location("/cart/items");

            for (int load = 0; load < 2; load++) {
                client.get()
                        .uri("/cart/items")
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(String.class)
                        .value(html -> {
                            if (quantity == 0) {
                                assertThat(html).doesNotContain("Coffee", "action=\"/buy\"");
                            } else {
                                assertThat(html).contains("Coffee", "<span>" + quantity + "</span>");
                            }
                        });
                if (quantity == 0) {
                    StepVerifier.create(cart.count())
                            .expectNext(0L)
                            .verifyComplete();
                } else {
                    StepVerifier.create(cart.findById(id))
                            .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(quantity))
                            .verifyComplete();
                }
            }
        }
    }

    @Test
    void renderedDetailButtonsChangeQuantityAndShowUpdatedItem() {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        String itemHtml = client.get()
                .uri("/items/{id}", id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        List<String> buttonUrls = Pattern.compile("formaction=\"([^\"]+)\"")
                .matcher(itemHtml)
                .results()
                .map(match -> HtmlUtils.htmlUnescape(match.group(1)))
                .toList();
        URI plusUrl = URI.create(buttonUrls.stream()
                .filter(url -> url.endsWith("?action=PLUS"))
                .findFirst()
                .orElseThrow());
        URI minusUrl = URI.create(buttonUrls.stream()
                .filter(url -> url.endsWith("?action=MINUS"))
                .findFirst()
                .orElseThrow());

        List<URI> actions = List.of(plusUrl, plusUrl, minusUrl);
        List<Integer> quantities = List.of(1, 2, 1);
        for (int index = 0; index < actions.size(); index++) {
            int quantity = quantities.get(index);
            client.post()
                    .uri(actions.get(index))
                    .exchange()
                    .expectStatus()
                    .isSeeOther()
                    .expectHeader()
                    .location("/items/" + id);

            // Follow the redirect, then refresh the resulting page.
            for (int load = 0; load < 2; load++) {
                client.get()
                        .uri("/items/{id}", id)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(String.class)
                        .value(html -> assertThat(html).contains("Coffee", "<span>" + quantity + "</span>"));
                StepVerifier.create(cart.findById(id))
                        .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(quantity))
                        .verifyComplete();
            }
        }
    }

    @Test
    void emptyPurchaseReturns400WithoutPersisting() {
        client.post()
                .uri("/buy")
                .exchange()
                .expectStatus()
                .isBadRequest();
        StepVerifier.create(orders.count())
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void missingEntitiesReturn404WithoutChangingCart() {
        client.get()
                .uri("/items/999")
                .exchange()
                .expectStatus()
                .isNotFound();
        client.get()
                .uri("/orders/999")
                .exchange()
                .expectStatus()
                .isNotFound();
        client.post()
                .uri("/cart/items?id=999&action=PLUS")
                .exchange()
                .expectStatus()
                .isNotFound();
        StepVerifier.create(cart.count())
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void invalidCartQueryReturns400WithoutChangingDatabase() {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        client.post()
                .uri("/cart/items?id={id}&action=UNKNOWN", id)
                .exchange()
                .expectStatus()
                .isBadRequest();
        StepVerifier.create(cart.count())
                .expectNext(0L)
                .verifyComplete();
    }
}
