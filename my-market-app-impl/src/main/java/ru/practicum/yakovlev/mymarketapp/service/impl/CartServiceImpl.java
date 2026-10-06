package ru.practicum.yakovlev.mymarketapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
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
public class CartServiceImpl implements CartService {
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    @Override
    public Mono<CartDto> getCart() {
        return cartItemRepository.findAllWithItems()
                .collectList()
                .map(cartMapper::toDto);
    }

    @Override
    @Transactional
    public Mono<Void> updateItem(long itemId, CartAction action) {
        return findItemOrThrow(itemId)
                .flatMap(item -> switch (action) {
                    case PLUS -> addItem(item);
                    case MINUS -> reduceOrDeleteItem(itemId);
                    case DELETE -> deleteItem(itemId);
                });
    }

    private Mono<Void> addItem(Item item) {
        return cartItemRepository.increment(item.getId())
                .then();
    }

    private Mono<Void> reduceOrDeleteItem(long itemId) {
        return cartItemRepository.findById(itemId)
                .flatMap(this::removeOne);
    }

    private Mono<Void> removeOne(CartItem cartItem) {
        if (cartItem.getQuantity() == 1) {
            return cartItemRepository.delete(cartItem);
        }

        cartItem.decrement();
        return cartItemRepository.save(cartItem)
                .then();
    }

    private Mono<Void> deleteItem(long itemId) {
        return cartItemRepository.deleteById(itemId);
    }

    private Mono<Item> findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new NotFoundException("Item not found: " + itemId)));
    }
}
