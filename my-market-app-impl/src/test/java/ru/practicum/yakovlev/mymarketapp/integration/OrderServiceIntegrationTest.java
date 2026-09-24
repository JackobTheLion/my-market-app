package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class OrderServiceIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void purchaseCommitsOrderPositionsAndClearsCart() {
        Item coffee = itemRepository.saveAndFlush(item("Coffee", "12.50"));
        Item tea = itemRepository.saveAndFlush(item("Tea", "3.25"));
        cartService.updateItem(coffee.getId(), CartAction.PLUS);
        cartService.updateItem(coffee.getId(), CartAction.PLUS);
        cartService.updateItem(tea.getId(), CartAction.PLUS);
        long id = orderService.createOrder();

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderItemRepository.count()).isEqualTo(2);
        assertThat(cartItemRepository.count()).isZero();
        OrderDto dto = orderService.getOrder(id);
        assertThat(dto.items()).extracting(ru.practicum.yakovlev.mymarketapp.dto.OrderItemDto::id)
                .containsExactly(coffee.getId(), tea.getId());
        assertThat(dto.items()).extracting(ru.practicum.yakovlev.mymarketapp.dto.OrderItemDto::count).containsExactly(2, 1);
        assertThat(dto.totalSum()).isEqualByComparingTo("28.25");
    }

    @Test
    void catalogChangesLeaveOrderSnapshotUnchanged() {
        Item item = itemRepository.saveAndFlush(item("Coffee", "12.50"));
        cartService.updateItem(item.getId(), CartAction.PLUS);
        long id = orderService.createOrder();
        item.setTitle("Changed");
        item.setPrice(new java.math.BigDecimal("99.00"));
        item.setImagePath("changed/photo.jpg");
        itemRepository.saveAndFlush(item);
        OrderDto dto = orderService.getOrder(id);
        assertThat(dto.items().getFirst().title()).isEqualTo("Coffee");
        assertThat(dto.items().getFirst().price()).isEqualByComparingTo("12.50");
        assertThat(dto.items().getFirst().imagePath()).isEqualTo("images/demo/photo.jpg");
        assertThat(dto.totalSum()).isEqualByComparingTo("12.50");
    }

    @Test
    void emptyCartFailsWithoutCreatingOrder() {
        assertThatThrownBy(orderService::createOrder).isInstanceOf(EmptyCartException.class);
        assertThat(orderRepository.count()).isZero();
        assertThat(orderItemRepository.count()).isZero();
    }

    @Test
    void historyIsNewestFirstAndMissingOrderIsRejected() {
        long id = itemRepository.saveAndFlush(item("Coffee", "12.50")).getId();
        cartService.updateItem(id, CartAction.PLUS);
        long first = orderService.createOrder();
        cartService.updateItem(id, CartAction.PLUS);
        long second = orderService.createOrder();
        // Fix the timestamps so the assertion does not depend on the clock resolution.
        jdbc.update("update orders set created_at = timestamp '2025-01-01 00:00:00'");
        assertThat(orderService.getOrders().orders()).extracting(ru.practicum.yakovlev.mymarketapp.dto.OrderDto::id)
                .containsExactly(second, first);
        assertThat(orderService.getOrders().totalSum()).isEqualByComparingTo("25.00");
        assertThatThrownBy(() -> orderService.getOrder(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void failureWhileClearingCartRollsBackAlreadyFlushedOrderAndPositions() {
        long id = itemRepository.saveAndFlush(item("Coffee", "12.50")).getId();
        cartService.updateItem(id, CartAction.PLUS);
        jdbc.execute("""
                CREATE FUNCTION reject_cart_delete() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'simulated cart cleanup failure'; END;
                $$
                """);
        try {
            jdbc.execute("CREATE TRIGGER reject_cart_delete BEFORE DELETE ON cart_items FOR EACH ROW EXECUTE FUNCTION reject_cart_delete()");
            assertThatThrownBy(orderService::createOrder)
                    .isInstanceOf(org.springframework.dao.DataAccessException.class)
                    .hasStackTraceContaining("simulated cart cleanup failure");
            // Repository calls start fresh transactions, outside the failed service transaction.
            assertThat(orderRepository.count()).isZero();
            assertThat(orderItemRepository.count()).isZero();
            assertThat(cartItemRepository.findById(id).orElseThrow().getQuantity()).isEqualTo(1);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS reject_cart_delete ON cart_items");
            jdbc.execute("DROP FUNCTION IF EXISTS reject_cart_delete()");
        }
    }
}
