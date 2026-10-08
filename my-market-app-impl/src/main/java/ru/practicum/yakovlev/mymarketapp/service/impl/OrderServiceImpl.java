package ru.practicum.yakovlev.mymarketapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.dto.OrdersPageDto;
import ru.practicum.yakovlev.mymarketapp.exception.EmptyCartException;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.OrderMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Order;
import ru.practicum.yakovlev.mymarketapp.model.OrderItem;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderRepository;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;

    @Override
    public Mono<OrdersPageDto> getOrders() {
        return orderRepository.findAllByOrderByCreatedAtDescIdDesc()
                .collectList()
                .flatMap(orders -> {
                    if (orders.isEmpty()) {
                        return Mono.just(new OrdersPageDto(List.of(), BigDecimal.ZERO));
                    }
                    return orderItemRepository.findAllByOrderByOrderIdAsc()
                            .collectMultimap(OrderItem::getOrderId)
                            .map(itemsByOrder -> {
                                List<OrderDto> dtos = orders.stream()
                                        .map(order -> {
                                            order.setItems(List.copyOf(itemsByOrder.getOrDefault(order.getId(), List.of())));
                                            return orderMapper.toDto(order);
                                        }).toList();
                                return new OrdersPageDto(dtos, dtos.stream()
                                        .map(OrderDto::totalSum)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                            });
                });
    }

    @Override
    public Mono<OrderDto> getOrder(long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Order not found: " + id)))
                .flatMap(this::loadItems)
                .map(orderMapper::toDto);
    }

    private Mono<Order> loadItems(Order order) {
        return orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId()).collectList()
                .map(items -> {
                    order.setItems(items);
                    return order;
                });
    }

    @Override
    @Transactional
    public Mono<Long> createOrder() {
        return cartItemRepository.findAllWithItemsForUpdate()
                .collectList()
                .filter(cartItems -> !cartItems.isEmpty())
                .switchIfEmpty(Mono.error(new EmptyCartException()))
                .flatMap(this::saveOrder);
    }

    private Mono<Long> saveOrder(List<CartItem> cartItems) {
        return orderRepository.save(new Order())
                .flatMap(order -> {
                    List<OrderItem> orderItems = cartItems.stream()
                            .map(cartItem -> new OrderItem(order, cartItem.getItem(), cartItem.getQuantity()))
                            .toList();
                    return orderItemRepository.saveAll(orderItems)
                            .then(cartItemRepository.deletePurchasedItems(cartItems.stream()
                                    .map(CartItem::getItemId).toList()))
                            .flatMap(deleted -> deleted == cartItems.size()
                                    ? Mono.just(order.getId())
                                    : Mono.error(new IllegalStateException("Cart cleanup did not delete all purchased items")));
                });
    }
}
