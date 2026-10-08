package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.util.RaceTestUtils;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class CartServiceIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private ItemRepository items;
    @Autowired
    private CartItemRepository cart;
    @Autowired
    private CartService service;

    @RepeatedTest(100)
    void concurrentPlusesCreatePositionWithoutLosingUnits() {
        long id = items.save(item("Coffee", "12.50")).block().getId();

        Runnable plus = () -> service.updateItem(id, CartAction.PLUS).block(Duration.ofSeconds(10));
        RaceTestUtils.race(15, Schedulers.boundedElastic(),
                Collections.nCopies(40, plus).toArray(Runnable[]::new));

        StepVerifier.create(cart.findById(id))
                .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(40))
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }

    @RepeatedTest(100)
    void concurrentMinusesRemoveEveryUnit() {
        int quantity = 40;
        long id = items.save(item("Coffee", "12.50")).block().getId();
        sql("INSERT INTO cart_items (item_id, quantity) VALUES (" + id + ", " + quantity + ")").block();

        Runnable minus = () -> service.updateItem(id, CartAction.MINUS).block(Duration.ofSeconds(10));
        RaceTestUtils.race(15, Schedulers.boundedElastic(),
                Collections.nCopies(quantity, minus).toArray(Runnable[]::new));

        StepVerifier.create(cart.findById(id))
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }

    @RepeatedTest(100)
    void twoConcurrentMinusesRemoveBothUnits() {
        long id = items.save(item("Coffee", "12.50")).block().getId();
        sql("INSERT INTO cart_items (item_id, quantity) VALUES (" + id + ", 2)").block();

        Runnable minus = () -> service.updateItem(id, CartAction.MINUS).block(Duration.ofSeconds(10));
        RaceTestUtils.race(15, Schedulers.boundedElastic(), minus, minus);

        StepVerifier.create(cart.findById(id))
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }

    @RepeatedTest(100)
    void concurrentPlusAndMinusPreserveLastUnit() {
        long id = items.save(item("Coffee", "12.50")).block().getId();
        sql("INSERT INTO cart_items (item_id, quantity) VALUES (" + id + ", 1)").block();

        RaceTestUtils.race(15, Schedulers.boundedElastic(),
                () -> service.updateItem(id, CartAction.PLUS).block(Duration.ofSeconds(10)),
                () -> service.updateItem(id, CartAction.MINUS).block(Duration.ofSeconds(10)));

        StepVerifier.create(cart.findById(id))
                .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(1))
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }

    @Test
    void plusMinusAndDeletePersistChanges() {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();

        service.updateItem(id, CartAction.MINUS)
                .block();
        service.updateItem(id, CartAction.PLUS)
                .then(service.updateItem(id, CartAction.PLUS))
                .block();

        StepVerifier.create(cart.findById(id))
                .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(2))
                .verifyComplete();
        service.updateItem(id, CartAction.MINUS)
                .block();
        StepVerifier.create(cart.findById(id))
                .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(1))
                .verifyComplete();
        service.updateItem(id, CartAction.MINUS)
                .then(service.updateItem(id, CartAction.MINUS))
                .block();
        StepVerifier.create(cart.count())
                .expectNext(0L)
                .verifyComplete();
        service.updateItem(id, CartAction.PLUS)
                .then(service.updateItem(id, CartAction.PLUS))
                .then(service.updateItem(id, CartAction.DELETE))
                .then(service.updateItem(id, CartAction.DELETE))
                .block();
        StepVerifier.create(service.getCart())
                .assertNext(dto -> {
                    assertThat(dto.items()).isEmpty();
                    assertThat(dto.total()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void totalUsesCurrentPricesAndItemsStayInIdOrder() {
        Item coffee = items.save(item("Coffee", "12.50"))
                .block();
        Item tea = items.save(item("Tea", "3.25"))
                .block();
        service.updateItem(tea.getId(), CartAction.PLUS)
                .then(service.updateItem(coffee.getId(), CartAction.PLUS))
                .then(service.updateItem(coffee.getId(), CartAction.PLUS))
                .block();
        StepVerifier.create(service.getCart())
                .assertNext(dto -> {
                    assertThat(dto.total()).isEqualByComparingTo("28.25");
                    assertThat(dto.items())
                            .extracting(ItemDto::id)
                            .containsExactly(coffee.getId(), tea.getId());
                })
                .verifyComplete();
        coffee.setPrice(new BigDecimal("20.00"));
        items.save(coffee)
                .block();
        StepVerifier.create(service.getCart())
                .assertNext(dto -> assertThat(dto.total()).isEqualByComparingTo("43.25"))
                .verifyComplete();
    }

    @Test
    void missingItemLeavesExistingCartIntact() {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        service.updateItem(id, CartAction.PLUS)
                .block();
        StepVerifier.create(service.updateItem(Long.MAX_VALUE, CartAction.PLUS))
                .expectError(NotFoundException.class)
                .verify();
        StepVerifier.create(cart.findById(id))
                .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void overflowRollsBackQuantityChange() {
        long id = items.save(item("Coffee", "12.50"))
                .block()
                .getId();
        service.updateItem(id, CartAction.PLUS)
                .block();
        sql("UPDATE cart_items SET quantity = 2147483647 WHERE item_id = " + id)
                .block();
        StepVerifier.create(service.updateItem(id, CartAction.PLUS))
                .expectError(DataAccessException.class)
                .verify();
        StepVerifier.create(cart.findById(id))
                .assertNext(entry -> assertThat(entry.getQuantity()).isEqualTo(Integer.MAX_VALUE))
                .verifyComplete();
    }

}
