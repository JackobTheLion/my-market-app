package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.TEST_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class ItemCacheIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private ItemRepository items;
    @Autowired
    private ItemService catalog;
    @Autowired
    private CartService cart;

    @Test
    void cardUsesRedisButCartQuantityRemainsFresh() {
        long id = items.save(item("Coffee", "12.50")).block().getId();
        assertThat(catalog.getItem(TEST_USER_ID, id).block().title()).isEqualTo("Coffee");
        sql("UPDATE items SET title = 'Changed' WHERE id = " + id).block();
        cart.updateItem(TEST_USER_ID, id, CartAction.PLUS).block();
        assertThat(catalog.getItem(TEST_USER_ID, id).block().title()).isEqualTo("Coffee");
        assertThat(catalog.getItem(TEST_USER_ID, id).block().count()).isEqualTo(1);
        cart.updateItem(TEST_USER_ID, id, CartAction.PLUS).block();
        assertThat(catalog.getItem(TEST_USER_ID, id).block().count()).isEqualTo(2);
    }

    @Test
    void catalogPagesAreCachedAndDifferentSearchSortAndPagingRemainIndependent() {
        long coffee = items.save(item("Coffee", "12.50")).block().getId();
        items.save(item("Tea", "3.25")).block();
        assertThat(catalog.getItems(TEST_USER_ID, "", ItemSort.PRICE, 1, 2).block().items()).extracting(product -> product.title())
                .containsExactly("Tea", "Coffee");
        sql("UPDATE items SET title = 'Changed' WHERE id = " + coffee).block();
        assertThat(catalog.getItems(TEST_USER_ID, "", ItemSort.PRICE, 1, 2).block().items()).extracting(product -> product.title())
                .containsExactly("Tea", "Coffee");
        assertThat(catalog.getItems(TEST_USER_ID, "Changed", ItemSort.ALPHA, 1, 5).block().items())
                .singleElement().satisfies(product -> assertThat(product.title()).isEqualTo("Changed"));
        assertThat(catalog.getItems(TEST_USER_ID, "", ItemSort.PRICE, 2, 1).block().items()).singleElement()
                .satisfies(product -> assertThat(product.title()).isEqualTo("Changed"));
    }

    @Test
    void cartLoadsProductFieldsThroughRedisAndQuantitiesFromPostgres() {
        long id = items.save(item("Coffee", "12.50")).block().getId();
        cart.updateItem(TEST_USER_ID, id, CartAction.PLUS).block();
        assertThat(cart.getCart(TEST_USER_ID).block().total()).isEqualByComparingTo("12.50");
        sql("UPDATE items SET price = 99 WHERE id = " + id).block();
        cart.updateItem(TEST_USER_ID, id, CartAction.PLUS).block();
        assertThat(cart.getCart(TEST_USER_ID).block().total()).isEqualByComparingTo("25.00");
    }
}
