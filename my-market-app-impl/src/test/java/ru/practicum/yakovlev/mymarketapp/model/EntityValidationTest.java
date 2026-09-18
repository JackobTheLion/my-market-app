package ru.practicum.yakovlev.mymarketapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class EntityValidationTest {

    @Test
    void itemRejectsNonPositiveOrMissingPrice() {
        assertThatThrownBy(() -> new Item("Coffee", "Arabica", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Item("Coffee", "Arabica", null, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> item("Coffee", "12.50").setPrice(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cartItemRejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> new CartItem(item("Coffee", "12.50"), 0))
                .isInstanceOf(IllegalArgumentException.class);

        CartItem cartItem = new CartItem(item("Coffee", "12.50"), 1);
        assertThatThrownBy(() -> cartItem.setQuantity(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(cartItem::decrement)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void orderItemRejectsNonPositiveQuantity() {
        Order order = new Order();

        assertThatThrownBy(() -> order.addItem(item("Coffee", "12.50"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
