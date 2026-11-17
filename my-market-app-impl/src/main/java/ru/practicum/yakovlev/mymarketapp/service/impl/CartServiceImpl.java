package ru.practicum.yakovlev.mymarketapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.CartMapper;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final ItemService itemService;

    @Override
    public Mono<CartDto> getCart(long userId) {
        return cartItemRepository.findAllByUserId(userId)
                .collectList()
                .flatMap(entries -> itemService.getItemsByIds(entries.stream().map(CartItem::getItemId).toList())
                        .map(products -> entries.stream()
                                .map(entry -> new CartItem(products.get(entry.getItemId()), entry.getQuantity()))
                                .toList()))
                .map(cartMapper::toDto);
    }

    @Override
    @Transactional
    public Mono<Void> updateItem(long userId, long itemId, CartAction action) {
        return findItemOrThrow(itemId)
                .flatMap(item -> switch (action) {
                    case PLUS -> cartItemRepository.increment(userId, item.getId()).then();
                    case MINUS -> reduceOrDeleteItem(userId, itemId);
                    case DELETE -> cartItemRepository.deleteItem(userId, itemId);
                });
    }

    private Mono<Void> reduceOrDeleteItem(long userId, long itemId) {
        return cartItemRepository.findItemForUpdate(userId, itemId)
                .flatMap(entry -> entry.getQuantity() > 1
                        ? cartItemRepository.decrement(userId, itemId).then()
                        : cartItemRepository.deleteLast(userId, itemId).then());
    }

    private Mono<Item> findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new NotFoundException("Item not found: " + itemId)));
    }
}
