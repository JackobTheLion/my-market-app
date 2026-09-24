package ru.practicum.yakovlev.mymarketapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.CartMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    @Override
    public CartDto getCart() {
        return cartMapper.toDto(cartItemRepository.findAllByOrderByItemIdAsc());
    }

    @Override
    @Transactional
    public void updateItem(long itemId, CartAction action) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        switch (action) {
            case PLUS -> addItem(item);
            case MINUS -> reduceOrDeleteItem(itemId);
            case DELETE -> deleteItem(itemId);
        }
    }

    private void addItem(Item item) {
        cartItemRepository.findById(item.getId())
                .ifPresentOrElse(
                        CartItem::increment,
                        () -> cartItemRepository.save(new CartItem(item, 1))
                );
    }

    private void reduceOrDeleteItem(long itemId) {
        cartItemRepository.findById(itemId)
                .ifPresent(this::removeOne);
    }

    private void removeOne(CartItem cartItem) {
        if (cartItem.getQuantity() == 1) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.decrement();
        }
    }

    private void deleteItem(long itemId) {
        cartItemRepository.deleteById(itemId);
    }

}
