package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.exception.EmptyCartException;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.OrderMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Order;
import ru.practicum.yakovlev.mymarketapp.model.OrderItem;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl service;

    @Test
    void createsOrderWithSnapshotAndClearsCartAfterSaving() {
        List<CartItem> cart = List.of(new CartItem(item(1, "Coffee", "12.50"), 2), new CartItem(item(2, "Tea", "3.25"), 3));
        when(cartItemRepository.findAllByOrderByItemIdAsc()).thenReturn(cart);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(42L);
            return order;
        });
        assertThat(service.createOrder()).isEqualTo(42);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        InOrder calls = inOrder(orderRepository, cartItemRepository);
        calls.verify(cartItemRepository).findAllByOrderByItemIdAsc();
        calls.verify(orderRepository).saveAndFlush(captor.capture());
        calls.verify(cartItemRepository).deleteAll(cart);
        Order order = captor.getValue();
        assertThat(order.getTotalSum()).isEqualByComparingTo("34.75");
        assertThat(order.getItems()).extracting(OrderItem::getTitle).containsExactly("Coffee", "Tea");
        assertThat(order.getItems()).extracting(OrderItem::getQuantity).containsExactly(2, 3);
        assertThat(order.getItems()).allSatisfy(entry -> assertThat(entry.getOrder()).isSameAs(order));
    }

    @Test
    void emptyCartDoesNotSaveOrDeleteAnything() {
        when(cartItemRepository.findAllByOrderByItemIdAsc()).thenReturn(List.of());
        assertThatThrownBy(service::createOrder).isInstanceOf(EmptyCartException.class);
        verifyNoInteractions(orderRepository);
        verify(cartItemRepository, never()).deleteAll(anyList());
    }

    @Test
    void failedSaveDoesNotClearCart() {
        when(cartItemRepository.findAllByOrderByItemIdAsc()).thenReturn(List.of(new CartItem(item(1, "Coffee", "12.50"), 1)));
        when(orderRepository.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("failed"));
        assertThatThrownBy(service::createOrder).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        verify(cartItemRepository, never()).deleteAll(anyList());
    }

    @Test
    void returnsMappedHistory() {
        List<Order> orders = List.of(new Order());
        List<OrderDto> dtos = List.of(new OrderDto(1, List.of(), BigDecimal.ZERO));
        when(orderRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(orders);
        when(orderMapper.toDtos(orders)).thenReturn(dtos);
        assertThat(service.getOrders()).isSameAs(dtos);
    }

    @Test
    void returnsMappedOrder() {
        Order order = new Order();
        OrderDto dto = new OrderDto(1, List.of(), BigDecimal.ZERO);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(dto);
        assertThat(service.getOrder(1)).isSameAs(dto);
    }

    @Test
    void missingOrderIsNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getOrder(99)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(orderMapper);
    }
}
