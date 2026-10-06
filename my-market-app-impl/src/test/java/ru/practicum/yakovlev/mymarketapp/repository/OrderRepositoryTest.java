package ru.practicum.yakovlev.mymarketapp.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.model.Order;
import ru.practicum.yakovlev.mymarketapp.model.OrderItem;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class OrderRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private ItemRepository items;
    @Autowired
    private OrderRepository orders;
    @Autowired
    private OrderItemRepository positions;

    @Test
    void loadsOrderAndPositionsUsingScalarForeignKeys() {
        Item coffee = items.save(item("Coffee", "12.50"))
                .block();
        Order order = orders.save(new Order())
                .block();
        positions.save(new OrderItem(order, coffee, 2))
                .block();
        StepVerifier.create(orders.findById(order.getId()))
                .assertNext(saved -> {
                    assertThat(saved.getCreatedAt()).isNotNull();
                    assertThat(saved.getItems()).isEmpty();
                })
                .verifyComplete();
        StepVerifier.create(positions.findAllByOrderIdOrderByIdAsc(order.getId()))
                .assertNext(entry -> {
                    assertThat(entry.getOrderId()).isEqualTo(order.getId());
                    assertThat(entry.getItemId()).isEqualTo(coffee.getId());
                    assertThat(entry.getTitle()).isEqualTo("Coffee");
                    assertThat(entry.getTotalPrice()).isEqualByComparingTo("25.00");
                })
                .verifyComplete();
    }
}
