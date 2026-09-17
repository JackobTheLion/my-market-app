package ru.practicum.yakovlev.mymarketapp.repository;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.support.JpaTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class CartItemRepositoryTest extends JpaTestSupport {
    @Autowired
    private CartItemRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void mapsPrimaryKeyToCatalogItemAndFetchesItemsInIdOrder() {
        Item first = entityManager.persist(item("Coffee", "12.50"));
        Item second = entityManager.persist(item("Tea", "3.25"));
        entityManager.persist(new CartItem(second, 3));
        entityManager.persist(new CartItem(first, 2));
        entityManager.flush();
        entityManager.clear();
        List<CartItem> entries = repository.findAllByOrderByItemIdAsc();
        assertThat(entries).extracting(CartItem::getItemId).containsExactly(first.getId(), second.getId());
        assertThat(entries).extracting(CartItem::getQuantity).containsExactly(2, 3);
        assertThat(entries).allSatisfy(entry -> assertThat(Hibernate.isInitialized(entry.getItem())).isTrue());
        entityManager.clear();
        assertThat(entries.getFirst().getItem().getTitle()).isEqualTo("Coffee");
    }

    @Test
    void dirtyCheckingPersistsQuantityWithoutSave() {
        Item item = entityManager.persist(item("Coffee", "12.50"));
        entityManager.persistAndFlush(new CartItem(item, 1));
        entityManager.clear();
        repository.findById(item.getId()).orElseThrow().increment();
        entityManager.flush();
        entityManager.clear();
        assertThat(repository.findById(item.getId()).orElseThrow().getQuantity()).isEqualTo(2);
    }

    @Test
    void deletingCatalogItemCascadesToCartInDatabase() {
        Item item = entityManager.persist(item("Coffee", "12.50"));
        entityManager.persistAndFlush(new CartItem(item, 1));
        entityManager.getEntityManager().createNativeQuery("delete from items where id = :id")
                .setParameter("id", item.getId()).executeUpdate();
        entityManager.clear();
        assertThat(repository.findById(item.getId())).isEmpty();
    }
}
