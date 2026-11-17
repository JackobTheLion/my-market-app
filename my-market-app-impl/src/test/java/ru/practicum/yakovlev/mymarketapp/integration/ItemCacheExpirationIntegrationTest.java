package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.time.Duration;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.TEST_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

@TestPropertySource(properties = "application.cache.product-ttl=PT1S")
class ItemCacheExpirationIntegrationTest extends IntegrationTestSupport {
    @Autowired private ItemRepository items;
    @Autowired private ItemService catalog;
    @Autowired private CartService cart;

    @Test
    void expiredCardPageAndCartReloadProductFromDatabase() {
        long id = items.save(item("Coffee", "12.50")).block().getId();
        cart.updateItem(TEST_USER_ID, id, CartAction.PLUS).block();
        catalog.getItem(TEST_USER_ID, id).block();
        catalog.getItems(TEST_USER_ID, "", ItemSort.NO, 1, 10).block();
        cart.getCart(TEST_USER_ID).block();
        sql("UPDATE items SET title = 'Changed', price = 99 WHERE id = " + id).block();
        Mono.delay(Duration.ofMillis(1200)).block();
        assertThat(catalog.getItem(TEST_USER_ID, id).block().title()).isEqualTo("Changed");
        assertThat(catalog.getItems(TEST_USER_ID, "", ItemSort.NO, 1, 10).block().items().getFirst().title()).isEqualTo("Changed");
        assertThat(cart.getCart(TEST_USER_ID).block().total()).isEqualByComparingTo("99.00");
    }
}
