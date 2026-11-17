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

import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.TEST_USER_ID;
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
        when(itemRepository.findById(1L)).thenReturn(Mono.just(coffee));
        when(cartItemRepository.findItemForUpdate(TEST_USER_ID, 1L)).thenReturn(Mono.just(new CartItem(coffee, 1)));
        when(cartItemRepository.deleteLast(TEST_USER_ID, 1L)).thenReturn(Mono.just(1));
        StepVerifier.create(service.updateItem(TEST_USER_ID, 1, CartAction.MINUS))
                .verifyComplete();
        verify(cartItemRepository).deleteLast(TEST_USER_ID, 1L);
        verify(cartItemRepository, never()).findItem(anyLong(), anyLong());
    }

    @Test
    void minusDecrementsLargerQuantity() {
        Item coffee = item(1, "Coffee", "12.50");
        when(itemRepository.findById(1L)).thenReturn(Mono.just(coffee));
        when(cartItemRepository.findItemForUpdate(TEST_USER_ID, 1L)).thenReturn(Mono.just(new CartItem(coffee, 2)));
        when(cartItemRepository.decrement(TEST_USER_ID, 1L)).thenReturn(Mono.just(1));
        StepVerifier.create(service.updateItem(TEST_USER_ID, 1, CartAction.MINUS))
                .verifyComplete();
        verify(cartItemRepository).decrement(TEST_USER_ID, 1L);
        verify(cartItemRepository, never()).deleteLast(anyLong(), anyLong());
    }

    @Test
    void minusOnMissingPositionCompletes() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1, "Coffee", "12.50")));
        when(cartItemRepository.findItemForUpdate(TEST_USER_ID, 1L)).thenReturn(Mono.empty());
        StepVerifier.create(service.updateItem(TEST_USER_ID, 1, CartAction.MINUS))
                .verifyComplete();
        verify(cartItemRepository, never()).decrement(anyLong(), anyLong());
        verify(cartItemRepository, never()).deleteLast(anyLong(), anyLong());
    }

    @Test
    void deleteRemovesWholePosition() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1, "Coffee", "12.50")));
        when(cartItemRepository.deleteItem(TEST_USER_ID, 1L)).thenReturn(Mono.empty());
        StepVerifier.create(service.updateItem(TEST_USER_ID, 1, CartAction.DELETE))
                .verifyComplete();
    }

}
