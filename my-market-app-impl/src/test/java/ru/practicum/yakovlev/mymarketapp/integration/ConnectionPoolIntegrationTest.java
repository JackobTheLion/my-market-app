package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.time.Duration;

import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.TEST_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.r2dbc.pool.initial-size=1",
        "spring.r2dbc.pool.max-size=1",
        "spring.r2dbc.pool.max-acquire-time=5s"
})
class ConnectionPoolIntegrationTest extends IntegrationTestSupport {
    private static final int POSITIONS = 500;
    private static final int CONCURRENT_READS = 10;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ItemService itemService;

    @Test
    void concurrentCatalogReadsCompleteWithOnlyOneConnection() {
        sql("""
                INSERT INTO items (title, description, price)
                SELECT 'Product ' || position, '', 1.00 FROM generate_series(1, 500) AS position
                """)
                .then(sql("INSERT INTO cart_items (user_id, item_id, quantity) SELECT 1, id, 2 FROM items"))
                .block();

        StepVerifier.create(Flux.range(0, CONCURRENT_READS)
                        .flatMap(ignored -> itemService.getItems(TEST_USER_ID, null, ItemSort.NO, 1, POSITIONS), CONCURRENT_READS)
                        .collectList())
                .assertNext(pages -> {
                    assertThat(pages).hasSize(CONCURRENT_READS);
                    pages.forEach(page -> {
                        assertThat(page.items()).hasSize(POSITIONS);
                        assertThat(page.items())
                                .allSatisfy(item -> assertThat(item.count()).isEqualTo(2));
                        assertThat(page.paging().hasPrevious()).isFalse();
                        assertThat(page.paging().hasNext()).isFalse();
                    });
                })
                .expectComplete()
                .verify(Duration.ofSeconds(45));
    }

    @Test
    void createsOrderWithOnlyOneConnection() {
        sql("""
                INSERT INTO items (title, description, price)
                SELECT 'Product ' || position, '', 1.00 FROM generate_series(1, 500) AS position
                """)
                .then(sql("INSERT INTO cart_items (user_id, item_id, quantity) SELECT 1, id, 1 FROM items"))
                .block();

        StepVerifier.create(orderService.createOrder(TEST_USER_ID)
                        .flatMap(orderId -> orderService.getOrder(TEST_USER_ID, orderId)))
                .assertNext(order -> {
                    assertThat(order.items()).hasSize(POSITIONS);
                    assertThat(order.totalSum()).isEqualByComparingTo("500.00");
                })
                .expectComplete()
                .verify(Duration.ofSeconds(45));
        StepVerifier.create(cartService.getCart(TEST_USER_ID))
                .assertNext(cart -> assertThat(cart.items()).isEmpty())
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void concurrentCartReadsCompleteWithOnlyOneConnection() {
        sql("""
                INSERT INTO items (title, description, price)
                SELECT 'Product ' || position, '', 1.00 FROM generate_series(1, 500) AS position
                """)
                .then(sql("INSERT INTO cart_items (user_id, item_id, quantity) SELECT 1, id, 1 FROM items"))
                .block();

        // The cart and its items are loaded with one JOIN, so one connection must suffice.
        StepVerifier.create(Flux.range(0, CONCURRENT_READS)
                        .flatMap(ignored -> cartService.getCart(TEST_USER_ID), CONCURRENT_READS)
                        .collectList())
                .assertNext(carts -> {
                    assertThat(carts).hasSize(CONCURRENT_READS);
                    carts.forEach(cart -> {
                        assertThat(cart.items()).hasSize(POSITIONS);
                        assertThat(cart.items())
                                .extracting(ItemDto::id)
                                .isSorted();
                        assertThat(cart.total()).isEqualByComparingTo("500.00");
                    });
                })
                .expectComplete()
                .verify(Duration.ofSeconds(45));
    }

    @Test
    void concurrentHistoryReadsCompleteWithOnlyOneConnection() {
        sql("INSERT INTO items (title, description, price) VALUES ('Product', '', 1.00)")
                .then(sql("""
                        INSERT INTO orders (created_at, user_id)
                        SELECT CURRENT_TIMESTAMP, %d FROM generate_series(1, 500)
                        """.formatted(TEST_USER_ID)))
                .then(sql("""
                        INSERT INTO order_items (order_id, item_id, title, price, quantity)
                        SELECT orders.id, items.id, items.title, items.price, 1 FROM orders CROSS JOIN items
                        """))
                .block();

        StepVerifier.create(Flux.range(0, CONCURRENT_READS)
                        .flatMap(ignored -> orderService.getOrders(TEST_USER_ID), CONCURRENT_READS)
                        .collectList())
                .assertNext(pages -> {
                    assertThat(pages).hasSize(CONCURRENT_READS);
                    pages.forEach(page -> {
                        assertThat(page.orders()).hasSize(POSITIONS);
                        assertThat(page.orders())
                                .extracting(OrderDto::id)
                                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
                        assertThat(page.orders())
                                .allSatisfy(order -> {
                                    assertThat(order.items()).hasSize(1);
                                    assertThat(order.totalSum()).isEqualByComparingTo("1.00");
                                });
                        assertThat(page.totalSum()).isEqualByComparingTo("500.00");
                    });
                })
                .expectComplete()
                .verify(Duration.ofSeconds(45));
    }
}
