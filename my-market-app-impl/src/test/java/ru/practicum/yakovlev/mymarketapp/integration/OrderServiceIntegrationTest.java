package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.util.RaceTestUtils;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.dto.OrderItemDto;
import ru.practicum.yakovlev.mymarketapp.exception.EmptyCartException;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class OrderServiceIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private OrderService service;
    @Autowired
    private CartService cart;
    @Autowired
    private ItemRepository items;
    @Autowired
    private CartItemRepository cartItems;
    @Autowired
    private OrderRepository orders;
    @Autowired
    private OrderItemRepository positions;

    @RepeatedTest(value = 100, failureThreshold = 1)
    void concurrentPurchasesOfTwoItemsCreateOnlyOneCompleteOrder() {
        Duration timeout = Duration.ofSeconds(10);
        long coffeeId = items.save(item("Coffee", "12.50")).block(timeout).getId();
        long teaId = items.save(item("Tea", "3.25")).block(timeout).getId();
        cart.updateItem(teaId, CartAction.PLUS)
                .then(cart.updateItem(coffeeId, CartAction.PLUS))
                .then(cart.updateItem(coffeeId, CartAction.PLUS))
                .block(timeout);
        Queue<Signal<Long>> results = new ConcurrentLinkedQueue<>();
        Runnable checkout = () -> results.add(service.createOrder()
                .materialize()
                .block(timeout));
        RaceTestUtils.race(15, Schedulers.boundedElastic(), checkout, checkout);

        assertThat(results).hasSize(2);
        assertThat(results)
                .filteredOn(Signal::isOnNext)
                .hasSize(1);
        assertThat(results)
                .filteredOn(Signal::isOnError)
                .singleElement()
                .satisfies(signal ->
                        assertThat(signal.getThrowable()).isInstanceOf(EmptyCartException.class));

        long orderId = results.stream().filter(Signal::isOnNext).findFirst().orElseThrow().get();
        OrderDto order = service.getOrder(orderId).block(timeout);
        assertThat(order.items())
                .extracting(OrderItemDto::id, OrderItemDto::count)
                .containsExactly(tuple(coffeeId, 2), tuple(teaId, 1));
        assertThat(order.totalSum()).isEqualByComparingTo("28.25");
        assertThat(orders.count().block(timeout)).isEqualTo(1L);
        assertThat(positions.count().block(timeout)).isEqualTo(2L);
        assertThat(cartItems.count().block(timeout)).isZero();
    }

    @RepeatedTest(value = 100, failureThreshold = 1)
    void concurrentCartIncrementAndPurchasePreserveQuantity() {
        assertConcurrentQuantityUpdateAndPurchase(CartAction.PLUS, false);
    }

    @RepeatedTest(value = 100, failureThreshold = 1)
    void concurrentCartIncrementAndPurchaseOfTwoItemsPreserveQuantity() {
        assertConcurrentQuantityUpdateAndPurchase(CartAction.PLUS, true);
    }

    @RepeatedTest(value = 100, failureThreshold = 1)
    void concurrentCartDecrementAndPurchaseOfTwoItemsPreserveQuantity() {
        assertConcurrentQuantityUpdateAndPurchase(CartAction.MINUS, true);
    }

    private void assertConcurrentQuantityUpdateAndPurchase(CartAction action, boolean twoItems) {
        Duration timeout = Duration.ofSeconds(10);
        long coffeeId = items.save(item("Coffee", "12.50")).block(timeout).getId();
        Long teaId = twoItems ? items.save(item("Tea", "3.25")).block(timeout).getId() : null;
        if (twoItems) {
            cart.updateItem(teaId, CartAction.PLUS).block(timeout);
        }
        cart.updateItem(coffeeId, CartAction.PLUS).block(timeout);
        if (action == CartAction.MINUS) {
            cart.updateItem(coffeeId, CartAction.PLUS).block(timeout);
        }
        AtomicReference<Long> orderId = new AtomicReference<>();

        RaceTestUtils.race(15, Schedulers.boundedElastic(),
                () -> cart.updateItem(coffeeId, action).block(timeout),
                () -> orderId.set(service.createOrder().block(timeout)));

        OrderDto order = service.getOrder(orderId.get()).block(timeout);
        assertThat(order.items()).extracting(OrderItemDto::id)
                .containsExactlyElementsOf(twoItems ? List.of(coffeeId, teaId) : List.of(coffeeId));
        int purchasedQuantity = order.items().getFirst().count();
        assertThat(purchasedQuantity).isIn(1, 2);
        int remainingQuantity = action == CartAction.PLUS ? 2 - purchasedQuantity : 0;
        assertThat(cartItems.findById(coffeeId).map(entry -> entry.getQuantity())
                .defaultIfEmpty(0).block(timeout)).isEqualTo(remainingQuantity);
        assertThat(cartItems.count().block(timeout)).isEqualTo(remainingQuantity > 0 ? 1L : 0L);
        if (twoItems) {
            assertThat(order.items().get(1).count()).isEqualTo(1);
        }
        BigDecimal expectedTotal = new BigDecimal("12.50").multiply(BigDecimal.valueOf(purchasedQuantity))
                .add(twoItems ? new BigDecimal("3.25") : BigDecimal.ZERO);
        assertThat(order.totalSum()).isEqualByComparingTo(expectedTotal);
        assertThat(orders.count().block(timeout)).isEqualTo(1L);
        assertThat(positions.count().block(timeout)).isEqualTo(twoItems ? 2L : 1L);
    }

    @RepeatedTest(value = 100, failureThreshold = 1)
    void concurrentCartDeletionAndPurchaseOfTwoItemsPreserveOtherItem() {
        Duration timeout = Duration.ofSeconds(10);
        long coffeeId = items.save(item("Coffee", "12.50")).block(timeout).getId();
        long teaId = items.save(item("Tea", "3.25")).block(timeout).getId();
        cart.updateItem(teaId, CartAction.PLUS)
                .then(cart.updateItem(coffeeId, CartAction.PLUS))
                .block(timeout);
        AtomicReference<Long> orderId = new AtomicReference<>();

        RaceTestUtils.race(15, Schedulers.boundedElastic(),
                () -> cart.updateItem(coffeeId, CartAction.DELETE).block(timeout),
                () -> orderId.set(service.createOrder().block(timeout)));

        OrderDto order = service.getOrder(orderId.get()).block(timeout);
        assertThat(order.items()).extracting(OrderItemDto::id)
                .isIn(List.of(teaId), List.of(coffeeId, teaId));
        assertThat(order.items()).extracting(OrderItemDto::count).containsOnly(1);
        assertThat(order.totalSum()).isEqualByComparingTo(order.items().size() == 2 ? "15.75" : "3.25");
        assertThat(orders.count().block(timeout)).isEqualTo(1L);
        assertThat(positions.count().block(timeout)).isEqualTo((long) order.items().size());
        assertThat(cartItems.count().block(timeout)).isZero();
    }

    @Test
    void purchaseCommitsPositionsAndClearsCart() {
        Item coffee = items.save(item("Coffee", "12.50"))
                .block();
        Item tea = items.save(item("Tea", "3.25"))
                .block();
        cart.updateItem(coffee.getId(), CartAction.PLUS)
                .then(cart.updateItem(coffee.getId(), CartAction.PLUS))
                .then(cart.updateItem(tea.getId(), CartAction.PLUS))
                .block();
        long id = service.createOrder()
                .block();
        StepVerifier.create(orders.count())
                .expectNext(1L)
                .verifyComplete();
        StepVerifier.create(positions.count())
                .expectNext(2L)
                .verifyComplete();
        StepVerifier.create(cartItems.count())
                .expectNext(0L)
                .verifyComplete();
        StepVerifier.create(service.getOrder(id))
                .assertNext(dto -> {
                    assertThat(dto.items())
                            .extracting(OrderItemDto::id)
                            .containsExactly(coffee.getId(), tea.getId());
                    assertThat(dto.items())
                            .extracting(OrderItemDto::count)
                            .containsExactly(2, 1);
                    assertThat(dto.totalSum()).isEqualByComparingTo("28.25");
                })
                .verifyComplete();
    }

    @Test
    void catalogEditsLeaveSnapshotUnchanged() {
        Item coffee = items.save(item("Coffee", "12.50"))
                .block();
        cart.updateItem(coffee.getId(), CartAction.PLUS)
                .block();
        long id = service.createOrder()
                .block();
        coffee.setTitle("Changed");
        coffee.setPrice(new BigDecimal("99.00"));
        coffee.setImagePath("changed/photo.jpg");
        items.save(coffee)
                .block();
        StepVerifier.create(service.getOrder(id))
                .assertNext(dto -> {
                    assertThat(dto.items().getFirst().title()).isEqualTo("Coffee");
                    assertThat(dto.items().getFirst().price()).isEqualByComparingTo("12.50");
                    assertThat(dto.items().getFirst().imagePath()).isEqualTo("images/demo/photo.jpg");
                    assertThat(dto.totalSum()).isEqualByComparingTo("12.50");
                })
                .verifyComplete();
    }

    @Test
    void emptyCartDoesNotCreateOrder() {
        StepVerifier.create(service.createOrder())
                .expectError(EmptyCartException.class)
                .verify();
        StepVerifier.create(orders.count())
                .expectNext(0L)
                .verifyComplete();
        StepVerifier.create(positions.count())
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void historyIsNewestFirstAndMissingOrderIsRejected() {
        long itemId = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        cart.updateItem(itemId, CartAction.PLUS)
                .block();
        long first = service.createOrder()
                .block();
        cart.updateItem(itemId, CartAction.PLUS)
                .block();
        long second = service.createOrder()
                .block();
        sql("UPDATE orders SET created_at = timestamp '2025-01-01 00:00:00'")
                .block();
        StepVerifier.create(service.getOrders())
                .assertNext(page -> {
                    assertThat(page.orders())
                            .extracting(OrderDto::id)
                            .containsExactly(second, first);
                    assertThat(page.totalSum()).isEqualByComparingTo("25.00");
                })
                .verifyComplete();
        StepVerifier.create(service.getOrder(Long.MAX_VALUE))
                .expectError(NotFoundException.class)
                .verify();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cleanupFailureRollsBackOrderAndPositionsAndPreservesCart(boolean skipDelete) {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        cart.updateItem(id, CartAction.PLUS)
                .block();
        sql("""
                CREATE FUNCTION reject_cart_delete() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN %s END;
                $$
                """.formatted(skipDelete ? "RETURN NULL;" : "RAISE EXCEPTION 'simulated cart cleanup failure';"))
                .block();
        try {
            sql("CREATE TRIGGER reject_cart_delete BEFORE DELETE ON cart_items FOR EACH ROW EXECUTE FUNCTION reject_cart_delete()")
                    .block();
            StepVerifier.create(service.createOrder())
                    .expectErrorSatisfies(error ->
                            assertThat(error)
                                    .isInstanceOf(skipDelete ? IllegalStateException.class : DataAccessException.class)
                                    .hasStackTraceContaining(skipDelete
                                            ? "Cart cleanup did not delete all purchased items"
                                            : "simulated cart cleanup failure"))
                    .verify();
            StepVerifier.create(orders.count())
                    .expectNext(0L)
                    .verifyComplete();
            StepVerifier.create(positions.count())
                    .expectNext(0L)
                    .verifyComplete();
            StepVerifier.create(cartItems.findById(id))
                    .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(1))
                    .verifyComplete();
        } finally {
            sql("DROP TRIGGER IF EXISTS reject_cart_delete ON cart_items")
                    .then(sql("DROP FUNCTION IF EXISTS reject_cart_delete()"))
                    .block();
        }
    }

    @Test
    void positionFailureRollsBackOrderAndKeepsCart() {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        cart.updateItem(id, CartAction.PLUS)
                .block();
        sql("""
                CREATE FUNCTION reject_order_item() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'simulated position failure'; END;
                $$
                """)
                .block();
        try {
            sql("CREATE TRIGGER reject_order_item BEFORE INSERT ON order_items FOR EACH ROW EXECUTE FUNCTION reject_order_item()")
                    .block();
            StepVerifier.create(service.createOrder())
                    .expectError(DataAccessException.class)
                    .verify();
            StepVerifier.create(orders.count())
                    .expectNext(0L)
                    .verifyComplete();
            StepVerifier.create(cartItems.count())
                    .expectNext(1L)
                    .verifyComplete();
        } finally {
            sql("DROP TRIGGER IF EXISTS reject_order_item ON order_items")
                    .then(sql("DROP FUNCTION IF EXISTS reject_order_item()"))
                    .block();
        }
    }

}
