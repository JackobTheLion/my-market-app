package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.CartMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.itemDto;

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
    void returnsMappedCart() {
        List<CartItem> entries = List.of(new CartItem(item(1, "Coffee", "12.50"), 2));
        CartDto dto = new CartDto(List.of(itemDto(1, 2)), new BigDecimal("25.00"));
        when(cartItemRepository.findAllByOrderByItemIdAsc()).thenReturn(entries);
        when(cartMapper.toDto(entries)).thenReturn(dto);
        assertThat(service.getCart()).isSameAs(dto);
    }

    @Test
    void plusCreatesNewCartPosition() {
        Item item = item(1, "Coffee", "12.50");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());
        service.updateItem(1, CartAction.PLUS);
        ArgumentCaptor<CartItem> captor = org.mockito.ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getItem()).isSameAs(item);
        assertThat(captor.getValue().getQuantity()).isEqualTo(1);
    }

    @Test
    void plusIncrementsExistingPosition() {
        Item item = item(1, "Coffee", "12.50");
        CartItem entry = new CartItem(item, 2);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(entry));
        service.updateItem(1, CartAction.PLUS);
        assertThat(entry.getQuantity()).isEqualTo(3);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void minusDecrementsExistingPosition() {
        Item item = item(1, "Coffee", "12.50");
        CartItem entry = new CartItem(item, 2);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(entry));
        service.updateItem(1, CartAction.MINUS);
        assertThat(entry.getQuantity()).isEqualTo(1);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void minusDeletesLastUnit() {
        Item item = item(1, "Coffee", "12.50");
        CartItem entry = new CartItem(item, 1);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(entry));
        service.updateItem(1, CartAction.MINUS);
        verify(cartItemRepository).delete(entry);
    }

    @Test
    void minusForAbsentPositionDoesNothing() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1, "Coffee", "12.50")));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());
        service.updateItem(1, CartAction.MINUS);
        verify(cartItemRepository, never()).delete(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void deleteRemovesWholePosition() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1, "Coffee", "12.50")));
        service.updateItem(1, CartAction.DELETE);
        verify(cartItemRepository).deleteById(1L);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(CartAction.class)
    void rejectsMissingCatalogItemBeforeChangingCart(CartAction action) {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateItem(99, action)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void rejectsQuantityOverflowWithoutChangingPosition() {
        Item item = item(1, "Coffee", "12.50");
        CartItem entry = new CartItem(item, Integer.MAX_VALUE);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(entry));
        assertThatThrownBy(() -> service.updateItem(1, CartAction.PLUS)).isInstanceOf(ArithmeticException.class);
        assertThat(entry.getQuantity()).isEqualTo(Integer.MAX_VALUE);
    }
}
