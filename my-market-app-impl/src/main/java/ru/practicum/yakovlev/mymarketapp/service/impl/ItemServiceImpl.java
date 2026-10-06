package ru.practicum.yakovlev.mymarketapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;
import ru.practicum.yakovlev.mymarketapp.dto.PagingDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.ItemMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemMapper itemMapper;

    @Override
    public Mono<ItemPageDto> getItems(String search, ItemSort sort, int pageNumber, int pageSize) {
        String query = search == null ? "" : search.strip();
        PageRequest request = PageRequest.of(pageNumber - 1, pageSize, sort.getSort());
        return itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, request)
                .collectList()
                .flatMap(items -> Mono.zip(
                        itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query),
                        getCartCounts(items),
                        (total, counts) -> toPageDto(items, total, counts, pageNumber, pageSize)));
    }

    @Override
    public Mono<ItemDto> getItem(long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Item not found: " + id)))
                .flatMap(item -> cartItemRepository.findById(id)
                        .map(CartItem::getQuantity).defaultIfEmpty(0)
                        .map(count -> itemMapper.toDto(item, count)));
    }

    private Mono<Map<Long, Integer>> getCartCounts(List<Item> items) {
        if (items.isEmpty()) {
            return Mono.just(Map.of());
        }
        return cartItemRepository.findAllById(items.stream().map(Item::getId).toList())
                .collectMap(CartItem::getItemId, CartItem::getQuantity);
    }

    private ItemPageDto toPageDto(List<Item> items, long total, Map<Long, Integer> counts,
                                  int pageNumber, int pageSize) {
        List<ItemDto> dtos = items.stream()
                .map(item -> itemMapper.toDto(item, counts.getOrDefault(item.getId(), 0)))
                .toList();
        PagingDto paging = PagingDto.builder()
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .hasPrevious(pageNumber > 1)
                .hasNext(total > (long) pageNumber * pageSize)
                .build();
        return new ItemPageDto(dtos, paging);
    }
}
