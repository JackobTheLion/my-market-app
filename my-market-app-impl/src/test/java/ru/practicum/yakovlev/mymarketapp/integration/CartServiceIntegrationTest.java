package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class CartServiceIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private CartService cartService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void quantitiesAreCommittedAndLastMinusRemovesPosition() {
        long id = itemRepository.saveAndFlush(item("Coffee", "12.50")).getId();
        cartService.updateItem(id, CartAction.PLUS);
        cartService.updateItem(id, CartAction.PLUS);
        assertThat(cartItemRepository.findById(id).orElseThrow().getQuantity())
                .isEqualTo(2);

        cartService.updateItem(id, CartAction.MINUS);
        assertThat(cartItemRepository.findById(id).orElseThrow().getQuantity())
                .isEqualTo(1);

        cartService.updateItem(id, CartAction.MINUS);
        assertThat(cartItemRepository.findById(id))
                .isEmpty();

        cartService.updateItem(id, CartAction.MINUS);
        assertThat(cartService.getCart().total())
                .isEqualByComparingTo("0");
    }

    @Test
    void deleteRemovesPositionRegardlessOfQuantityAndIsIdempotent() {
        long id = itemRepository.saveAndFlush(item("Coffee", "12.50")).getId();

        cartService.updateItem(id, CartAction.PLUS);
        cartService.updateItem(id, CartAction.PLUS);
        cartService.updateItem(id, CartAction.DELETE);

        assertThat(cartItemRepository.count()).isZero();
    }

    @Test
    void cartTotalUsesCurrentCatalogPricesAndItemsAreInIdOrder() {
        Item coffee = itemRepository.saveAndFlush(item("Coffee", "12.50"));
        Item tea = itemRepository.saveAndFlush(item("Tea", "3.25"));
        cartService.updateItem(tea.getId(), CartAction.PLUS);
        cartService.updateItem(coffee.getId(), CartAction.PLUS);
        cartService.updateItem(coffee.getId(), CartAction.PLUS);

        assertThat(cartService.getCart().total())
                .isEqualByComparingTo("28.25");
        assertThat(cartService.getCart().items()).extracting(ru.practicum.yakovlev.mymarketapp.dto.ItemDto::id)
                .containsExactly(coffee.getId(), tea.getId());

        coffee.setPrice(new BigDecimal("20.00"));
        itemRepository.saveAndFlush(coffee);

        assertThat(cartService.getCart().total())
                .isEqualByComparingTo("43.25");
    }

    @Test
    void missingItemLeavesExistingCartIntact() {
        long id = itemRepository.saveAndFlush(item("Coffee", "12.50")).getId();

        cartService.updateItem(id, CartAction.PLUS);

        assertThatThrownBy(() -> cartService.updateItem(Long.MAX_VALUE, CartAction.PLUS))
                .isInstanceOf(NotFoundException.class);
        assertThat(cartItemRepository.count())
                .isEqualTo(1);
        assertThat(cartItemRepository.findById(id).orElseThrow().getQuantity())
                .isEqualTo(1);
    }

    @Test
    void overflowRollsBackQuantityChange() {
        Item item = itemRepository.saveAndFlush(item("Coffee", "12.50"));
        cartService.updateItem(item.getId(), CartAction.PLUS);
        CartItem cartItem = cartItemRepository.findById(item.getId()).orElseThrow();
        cartItem.setQuantity(Integer.MAX_VALUE);
        cartItemRepository.saveAndFlush(cartItem);

        assertThatThrownBy(() -> cartService.updateItem(item.getId(), CartAction.PLUS))
                .isInstanceOf(ArithmeticException.class);
        assertThat(cartItemRepository.findById(item.getId()).orElseThrow().getQuantity())
                .isEqualTo(Integer.MAX_VALUE);
    }
}
