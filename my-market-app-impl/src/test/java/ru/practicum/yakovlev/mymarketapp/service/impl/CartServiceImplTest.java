package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.mapper.CartMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CartMapper cartMapper;
    @InjectMocks
    private CartServiceImpl service;

    @Test
    void minusDeletesLastUnit() {
        Item coffee = item(1, "Coffee", "12.50");
        CartItem cartItem = new CartItem(coffee, 1);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(coffee));
        when(cartItemRepository.findById(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());
        StepVerifier.create(service.updateItem(1, CartAction.MINUS))
                .verifyComplete();
        verify(cartItemRepository)
                .delete(cartItem);
        verify(cartItemRepository, never())
                .save(any(CartItem.class));
    }

    @Test
    void minusDecrementsLargerQuantity() {
        Item coffee = item(1, "Coffee", "12.50");
        CartItem cartItem = new CartItem(coffee, 3);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(coffee));
        when(cartItemRepository.findById(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(Mono.just(cartItem));
        StepVerifier.create(service.updateItem(1, CartAction.MINUS))
                .verifyComplete();
        assertThat(cartItem.getQuantity()).isEqualTo(2);
        verify(cartItemRepository)
                .save(cartItem);
        verify(cartItemRepository, never())
                .delete(any(CartItem.class));
    }

    @Test
    void deleteRemovesWholePosition() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1, "Coffee", "12.50")));
        when(cartItemRepository.deleteById(1L)).thenReturn(Mono.empty());
        StepVerifier.create(service.updateItem(1, CartAction.DELETE))
                .verifyComplete();
    }

}
