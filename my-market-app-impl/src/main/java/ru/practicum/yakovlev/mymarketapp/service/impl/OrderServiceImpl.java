package ru.practicum.yakovlev.mymarketapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.dto.OrdersPageDto;
import ru.practicum.yakovlev.mymarketapp.exception.EmptyCartException;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.OrderMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Order;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderRepository;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;

    @Override
    public OrdersPageDto getOrders() {
        List<OrderDto> orders = orderMapper.toDtos(orderRepository.findAllByOrderByCreatedAtDescIdDesc());
        BigDecimal totalSum = orders.stream()
                .map(OrderDto::totalSum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrdersPageDto(orders, totalSum);
    }

    @Override
    public OrderDto getOrder(long id) {
        return orderMapper.toDto(orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id)));
    }

    @Override
    @Transactional
    public long createOrder() {
        List<CartItem> cartItems = cartItemRepository.findAllByOrderByItemIdAsc();
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }
        Order order = new Order();
        cartItems.forEach(cartItem -> order.addItem(cartItem.getItem(), cartItem.getQuantity()));
        Order saved = orderRepository.saveAndFlush(order);
        cartItemRepository.deleteAll(cartItems);
        return saved.getId();
    }
}
