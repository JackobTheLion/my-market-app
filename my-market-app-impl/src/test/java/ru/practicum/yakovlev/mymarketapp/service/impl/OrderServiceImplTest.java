package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.exception.EmptyCartException;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.OrderMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.model.Order;
import ru.practicum.yakovlev.mymarketapp.model.OrderItem;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private OrderMapper orderMapper;
    @InjectMocks
    private OrderServiceImpl service;

    @Test
    void savesSnapshotBeforeDeletingCart() {
        Item coffee = item(1, "Coffee", "12.50");
        when(cartItemRepository.findAllWithItemsForUpdate()).thenReturn(Flux.just(new CartItem(coffee, 2)));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(42L);
                    return Mono.just(order);
                });
        Sinks.Empty<Void> positionsSaved = Sinks.empty();
        when(orderItemRepository.saveAll(anyIterable()))
                .thenAnswer(invocation -> {
                    List<OrderItem> entries = invocation.getArgument(0);
                    assertThat(entries).hasSize(1);
                    OrderItem entry = entries.getFirst();
                    assertThat(entry.getOrderId()).isEqualTo(42L);
                    assertThat(entry.getItemId()).isEqualTo(1L);
                    assertThat(entry.getTitle()).isEqualTo("Coffee");
                    assertThat(entry.getQuantity()).isEqualTo(2);
                    assertThat(entry.getTotalPrice()).isEqualByComparingTo("25.00");
                    return Flux.just(entry)
                            .concatWith(positionsSaved.asMono()
                                    .thenMany(Flux.empty()));
                });
        AtomicBoolean cartDeleted = new AtomicBoolean();
        when(cartItemRepository.deletePurchasedItems(List.of(1L))).thenReturn(Mono.fromSupplier(() -> {
            cartDeleted.set(true);
            return 1;
        }));
        StepVerifier.create(service.createOrder())
                .then(() -> {
                    assertThat(cartDeleted).isFalse();
                    positionsSaved.tryEmitEmpty();
                })
                .expectNext(42L)
                .expectComplete()
                .verify(java.time.Duration.ofSeconds(5));
        assertThat(cartDeleted).isTrue();
        verify(cartItemRepository, never())
                .findAll();
    }

    @Test
    void emptyCartEmitsErrorWithoutSaving() {
        when(cartItemRepository.findAllWithItemsForUpdate()).thenReturn(Flux.empty());
        StepVerifier.create(service.createOrder())
                .expectError(EmptyCartException.class)
                .verify();
        verifyNoInteractions(orderRepository, orderItemRepository);
        verify(cartItemRepository, never())
                .deletePurchasedItems(anyList());
    }

    @Test
    void failedSaveDoesNotDeleteCart() {
        when(cartItemRepository.findAllWithItemsForUpdate()).thenReturn(Flux.just(new CartItem(item(1, "Coffee", "12.50"), 1)));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.error(new DataIntegrityViolationException("failed")));
        StepVerifier.create(service.createOrder())
                .expectError(DataIntegrityViolationException.class)
                .verify();
        verify(cartItemRepository, never())
                .deletePurchasedItems(anyList());
    }

    @Test
    void emptyHistoryHasZeroTotal() {
        when(orderRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(Flux.empty());
        StepVerifier.create(service.getOrders())
                .assertNext(page -> {
                    assertThat(page.orders()).isEmpty();
                    assertThat(page.totalSum()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void historyLoadsPositionsOnceAndGroupsThemWithoutChangingOrder() {
        Order oldest = new Order();
        oldest.setId(41L);
        Order newer = new Order();
        newer.setId(42L);
        Order empty = new Order();
        empty.setId(43L);
        Item coffee = item(1, "Coffee", "12.50");
        Item tea = item(2, "Tea", "3.25");
        OrderItem oldestCoffee = new OrderItem(oldest, coffee, 1);
        OrderItem oldestTea = new OrderItem(oldest, tea, 2);
        OrderItem newerCoffee = new OrderItem(newer, coffee, 2);
        when(orderRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(Flux.just(empty, newer, oldest));
        when(orderItemRepository.findAllByOrderByOrderIdAsc()).thenReturn(Flux.just(oldestCoffee, oldestTea, newerCoffee));
        when(orderMapper.toDto(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    return new OrderDto(order.getId(), List.of(), order.getTotalSum());
                });

        StepVerifier.create(service.getOrders())
                .assertNext(page -> {
                    assertThat(page.orders())
                            .extracting(OrderDto::id)
                            .containsExactly(43L, 42L, 41L);
                    assertThat(empty.getItems()).isEmpty();
                    assertThat(newer.getItems()).containsExactly(newerCoffee);
                    assertThat(oldest.getItems()).containsExactly(oldestCoffee, oldestTea);
                    assertThat(page.totalSum()).isEqualByComparingTo("44.00");
                })
                .verifyComplete();
        verify(orderItemRepository, times(1))
                .findAllByOrderByOrderIdAsc();
        verify(orderItemRepository, never())
                .findAllByOrderIdOrderByIdAsc(anyLong());
    }

    @Test
    void missingOrderEmitsNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getOrder(99))
                .expectError(NotFoundException.class)
                .verify();
        verifyNoInteractions(orderMapper, orderItemRepository);
    }
}
