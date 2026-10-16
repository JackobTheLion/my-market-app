package ru.practicum.yakovlev.mymarketapp.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class CartItemRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private ItemRepository items;
    @Autowired
    private CartItemRepository cart;

    @Test
    void assignedItemIdUpsertInsertsAndThenIncrements() {
        long id = items.save(item("Coffee", "12.50")).block().getId();
        cart.increment(id)
                .then(cart.increment(id))
                .block();

        StepVerifier.create(cart.findById(id))
                .assertNext(entry -> {
                    assertThat(entry.getItemId()).isEqualTo(id);
                    assertThat(entry.getQuantity()).isEqualTo(2);
                    assertThat(entry.getItem()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void minusSqlNeverCreatesZeroQuantity() {
        long id = items.save(item("Coffee", "12.50")).block().getId();
        cart.increment(id)
                .then(cart.increment(id))
                .block();
        StepVerifier.create(cart.deleteLast(id))
                .expectNext(0)
                .verifyComplete();
        StepVerifier.create(cart.decrement(id))
                .expectNext(1)
                .verifyComplete();
        StepVerifier.create(cart.deleteLast(id))
                .expectNext(1)
                .verifyComplete();
        StepVerifier.create(cart.findById(id))
                .verifyComplete();
    }

    @Test
    void joinLoadsAllItemFieldsAndQuantitiesInItemIdOrder() {
        Item coffee = items.save(item("Coffee", "12.50")).block();
        Item tea = item("Tea", "3.25");
        tea.setDescription("Green tea");
        tea.setImagePath(null);
        Item savedTea = items.save(tea)
                .block();
        cart.increment(savedTea.getId())
                .then(cart.increment(coffee.getId()))
                .then(cart.increment(coffee.getId()))
                .block();

        StepVerifier.create(cart.findAllWithItems())
                .assertNext(entry -> {
                    assertThat(entry.getItemId()).isEqualTo(coffee.getId());
                    assertThat(entry.getQuantity()).isEqualTo(2);
                    assertThat(entry.getItem().getId()).isEqualTo(coffee.getId());
                    assertThat(entry.getItem().getTitle()).isEqualTo("Coffee");
                    assertThat(entry.getItem().getDescription()).isEqualTo("Description of Coffee");
                    assertThat(entry.getItem().getImagePath()).isEqualTo("demo/photo.jpg");
                    assertThat(entry.getItem().getPrice()).isEqualByComparingTo("12.50");
                })
                .assertNext(entry -> {
                    assertThat(entry.getItemId()).isEqualTo(savedTea.getId());
                    assertThat(entry.getQuantity()).isEqualTo(1);
                    assertThat(entry.getItem().getId()).isEqualTo(savedTea.getId());
                    assertThat(entry.getItem().getTitle()).isEqualTo("Tea");
                    assertThat(entry.getItem().getDescription()).isEqualTo("Green tea");
                    assertThat(entry.getItem().getImagePath()).isNull();
                    assertThat(entry.getItem().getPrice()).isEqualByComparingTo("3.25");
                })
                .verifyComplete();
    }

}
