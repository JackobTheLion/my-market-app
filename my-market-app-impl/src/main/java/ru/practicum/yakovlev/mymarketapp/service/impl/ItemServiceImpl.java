package ru.practicum.yakovlev.mymarketapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemMapper itemMapper;

    @Override
    public ItemPageDto getItems(String search, ItemSort sort, int pageNumber, int pageSize) {
        PageRequest request = PageRequest.of(pageNumber - 1, pageSize, sort.getSort());
        String query = search == null ? "" : search.strip();
        Page<Item> page = query.isEmpty()
                ? itemRepository.findAll(request)
                : itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, request);

        Map<Long, Integer> counts = page.isEmpty() ? Map.of() : cartItemRepository
                .findAllById(page.getContent().stream().map(Item::getId).toList()).stream()
                .collect(Collectors.toMap(CartItem::getItemId, CartItem::getQuantity));
        List<ItemDto> items = page.getContent().stream()
                .map(item -> itemMapper.toDto(item, counts.getOrDefault(item.getId(), 0)))
                .toList();
        return new ItemPageDto(items,
                new PagingDto(pageSize, pageNumber, page.hasPrevious(), page.hasNext()));
    }

    @Override
    public ItemDto getItem(long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found: " + id));
        int count = cartItemRepository.findById(id).map(CartItem::getQuantity).orElse(0);
        return itemMapper.toDto(item, count);
    }
}
