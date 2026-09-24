package ru.practicum.yakovlev.mymarketapp.repository;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.model.Order;
import ru.practicum.yakovlev.mymarketapp.support.JpaTestSupport;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class PositiveValueConstraintTest extends JpaTestSupport {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void databaseRejectsNonPositiveItemPrice() {
        Item item = entityManager.persistAndFlush(item("Coffee", "12.50"));

        assertThatThrownBy(() -> executeUpdate("update items set price = 0 where id = :id", item.getId()))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void databaseRejectsNonPositiveCartQuantity() {
        Item item = entityManager.persist(item("Coffee", "12.50"));
        entityManager.persistAndFlush(new CartItem(item, 1));

        assertThatThrownBy(() -> executeUpdate("update cart_items set quantity = 0 where item_id = :id", item.getId()))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void databaseRejectsNonPositiveOrderItemPrice() {
        Order order = persistedOrder();

        assertThatThrownBy(() -> executeUpdate("update order_items set price = 0 where order_id = :id", order.getId()))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void databaseRejectsNonPositiveOrderItemQuantity() {
        Order order = persistedOrder();

        assertThatThrownBy(() -> executeUpdate("update order_items set quantity = 0 where order_id = :id", order.getId()))
                .isInstanceOf(ConstraintViolationException.class);
    }

    private Order persistedOrder() {
        Item item = entityManager.persist(item("Coffee", "12.50"));
        Order order = new Order();
        order.addItem(item, 1);
        return entityManager.persistAndFlush(order);
    }

    private void executeUpdate(String sql, long id) {
        entityManager.getEntityManager().createNativeQuery(sql)
                .setParameter("id", id)
                .executeUpdate();
    }
}
