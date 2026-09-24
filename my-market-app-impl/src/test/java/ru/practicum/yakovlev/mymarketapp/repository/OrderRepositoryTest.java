package ru.practicum.yakovlev.mymarketapp.repository;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.model.Order;
import ru.practicum.yakovlev.mymarketapp.model.OrderItem;
import ru.practicum.yakovlev.mymarketapp.support.JpaTestSupport;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class OrderRepositoryTest extends JpaTestSupport {
    @Autowired
    private OrderRepository repository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void cascadesOrderPositionsAndFetchesThemInPositionIdOrder() {
        Item coffee = entityManager.persist(item("Coffee", "12.50"));
        Item tea = entityManager.persist(item("Tea", "3.25"));
        Order order = new Order();
        order.addItem(coffee, 2);
        order.addItem(tea, 3);
        Long id = repository.saveAndFlush(order).getId();
        entityManager.clear();
        Order loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(Hibernate.isInitialized(loaded.getItems())).isTrue();
        assertThat(loaded.getItems()).extracting(OrderItem::getTitle).containsExactly("Coffee", "Tea");
        assertThat(loaded.getTotalSum()).isEqualByComparingTo("34.75");
        assertThat(orderItemRepository.count()).isEqualTo(2);
        entityManager.clear();
        assertThat(loaded.getItems()).hasSize(2);
    }

    @Test
    void historyUsesTimestampThenIdDescendingAndFetchesPositions() {
        Item item = entityManager.persist(item("Coffee", "12.50"));
        Order old = new Order();
        old.addItem(item, 1);
        Order firstNew = new Order();
        firstNew.addItem(item, 2);
        Order secondNew = new Order();
        secondNew.addItem(item, 3);
        repository.saveAndFlush(old);
        repository.saveAndFlush(firstNew);
        repository.saveAndFlush(secondNew);
        old.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        firstNew.setCreatedAt(LocalDateTime.of(2025, 1, 2, 0, 0));
        secondNew.setCreatedAt(firstNew.getCreatedAt());
        entityManager.flush();
        entityManager.clear();
        List<Order> history = repository.findAllByOrderByCreatedAtDescIdDesc();
        assertThat(history).extracting(Order::getId).containsExactly(secondNew.getId(), firstNew.getId(), old.getId());
        assertThat(history).allSatisfy(order -> assertThat(Hibernate.isInitialized(order.getItems())).isTrue());
    }

    @Test
    void catalogChangesDoNotOverwriteOrderSnapshot() {
        Item item = entityManager.persist(item("Coffee", "12.50"));
        Order order = new Order();
        order.addItem(item, 2);
        repository.saveAndFlush(order);
        item.setTitle("New name");
        item.setPrice(new java.math.BigDecimal("99.00"));
        entityManager.flush();
        entityManager.clear();
        Order loaded = repository.findById(order.getId()).orElseThrow();
        assertThat(loaded.getItems().getFirst().getTitle()).isEqualTo("Coffee");
        assertThat(loaded.getItems().getFirst().getPrice()).isEqualByComparingTo("12.50");
        assertThat(loaded.getTotalSum()).isEqualByComparingTo("25.00");
    }

    @Test
    void databaseRejectsDuplicateCatalogItemInsideOrder() {
        Item item = entityManager.persist(item("Coffee", "12.50"));
        Order order = new Order();
        order.addItem(item, 1);
        order.addItem(item, 2);
        assertThatThrownBy(() -> repository.saveAndFlush(order))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void databasePreventsDeletingCatalogItemUsedByOrder() {
        Item item = entityManager.persist(item("Coffee", "12.50"));
        Order order = new Order();
        order.addItem(item, 1);
        repository.saveAndFlush(order);
        assertThatThrownBy(() -> entityManager.getEntityManager()
                .createNativeQuery("delete from items where id = :id").setParameter("id", item.getId()).executeUpdate())
                .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }
}
