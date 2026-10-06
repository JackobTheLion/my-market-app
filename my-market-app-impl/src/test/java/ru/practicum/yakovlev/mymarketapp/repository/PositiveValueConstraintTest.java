package ru.practicum.yakovlev.mymarketapp.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.model.Order;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class PositiveValueConstraintTest extends IntegrationTestSupport {
    @Autowired
    private ItemRepository items;
    @Autowired
    private OrderRepository orders;

    @Test
    void rejectsNonPositiveCatalogPriceInDatabase() {
        StepVerifier.create(sql("INSERT INTO items (title, description, price) VALUES ('Invalid', '', 0)"))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void rejectsNonPositiveCartQuantityInDatabase() {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        StepVerifier.create(sql("INSERT INTO cart_items (item_id, quantity) VALUES (" + id + ", 0)"))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void rejectsNonPositiveOrderQuantityAndPriceInDatabase() {
        long itemId = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        long orderId = orders.save(new Order())
                .block()
                .getId();
        String prefix = "INSERT INTO order_items (order_id, item_id, title, price, quantity) VALUES ("
                + orderId + ", " + itemId + ", 'Coffee', ";
        StepVerifier.create(sql(prefix + "0, 1)"))
                .expectError(DataIntegrityViolationException.class)
                .verify();
        StepVerifier.create(sql(prefix + "12.50, 0)"))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void foreignKeysAndUniqueOrderPositionAreEnforced() {
        StepVerifier.create(sql("INSERT INTO cart_items (item_id, quantity) VALUES (9223372036854775807, 1)"))
                .expectError(DataIntegrityViolationException.class)
                .verify();
        long itemId = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        long orderId = orders.save(new Order())
                .block()
                .getId();
        String insert = "INSERT INTO order_items (order_id, item_id, title, price, quantity) VALUES ("
                + orderId + ", " + itemId + ", 'Coffee', 12.50, 1)";
        sql(insert)
                .block();
        StepVerifier.create(sql(insert))
                .expectError(DataIntegrityViolationException.class)
                .verify();
        StepVerifier.create(items.deleteById(itemId))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }
}
