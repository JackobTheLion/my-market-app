package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {
    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        ItemMapper mapper = Mappers.getMapper(ItemMapper.class);
        ReflectionTestUtils.setField(mapper, "defaultImage", "default-image.svg");
        service = new ItemServiceImpl(itemRepository, cartItemRepository, mapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t\n"})
    void blankSearchUsesWholeCatalog(String search) {
        PageRequest request = PageRequest.of(0, 5, ItemSort.NO.getSort());
        when(itemRepository.findAll(request)).thenReturn(new PageImpl<>(List.of(), request, 0));
        ItemPageDto page = service.getItems(search, ItemSort.NO, 1, 5);
        assertThat(page.items()).isEmpty();
        assertThat(page.paging()).isEqualTo(new PagingDto(5, 1, false, false));
        verifyNoInteractions(cartItemRepository);
        verify(itemRepository, never()).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void trimsSearchAndPassesSortingAndZeroBasedPage() {
        PageRequest request = PageRequest.of(1, 2, ItemSort.PRICE.getSort());
        Item item = item(1, "Coffee", "12.50");
        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("Coffee", "Coffee", request))
                .thenReturn(new PageImpl<>(List.of(item), request, 5));
        CartItem cartItem = new CartItem(item, 4);
        cartItem.setItemId(1L); // @MapsId is populated by Hibernate only in persistence tests.
        when(cartItemRepository.findAllById(List.of(1L))).thenReturn(List.of(cartItem));
        ItemPageDto page = service.getItems("  Coffee  ", ItemSort.PRICE, 2, 2);
        assertThat(page.paging()).isEqualTo(new PagingDto(2, 2, true, true));
        assertThat(page.items().getFirst().getFirst().count()).isEqualTo(4);
    }

    @Test
    void splitsRowsAndPadsOnlyLastRow() {
        List<Item> items = List.of(item(1, "A", "1.00"), item(2, "B", "2.00"),
                item(3, "C", "3.00"), item(4, "D", "4.00"));
        PageRequest request = PageRequest.of(0, 5, ItemSort.ALPHA.getSort());
        when(itemRepository.findAll(request)).thenReturn(new PageImpl<>(items, request, 4));
        when(cartItemRepository.findAllById(List.of(1L, 2L, 3L, 4L))).thenReturn(List.of());
        ItemPageDto page = service.getItems(null, ItemSort.ALPHA, 1, 5);
        assertThat(page.items()).hasSize(2).allSatisfy(row -> assertThat(row).hasSize(3));
        assertThat(page.items().getFirst()).extracting(ItemDto::id).containsExactly(1L, 2L, 3L);
        assertThat(page.items().getLast()).extracting(ItemDto::id).containsExactly(4L, -1L, -1L);
        assertThat(page.items().getFirst()).allSatisfy(dto -> assertThat(dto.count()).isZero());
    }

    @Test
    void fullRowNeedsNoPadding() {
        PageRequest request = PageRequest.of(0, 3);
        when(itemRepository.findAll(request)).thenReturn(new PageImpl<>(List.of(
                item(1, "A", "1"), item(2, "B", "2"), item(3, "C", "3")), request, 3));
        when(cartItemRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of());
        assertThat(service.getItems(null, ItemSort.NO, 1, 3).items().getFirst())
                .extracting(ItemDto::id).containsExactly(1L, 2L, 3L);
    }

    @Test
    void itemIncludesCartQuantity() {
        Item item = item(1, "Coffee", "12.50");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(new CartItem(item, 3)));
        assertThat(service.getItem(1).count()).isEqualTo(3);
        assertThat(service.getItem(1).imgPath()).isEqualTo("images/demo/photo.jpg");
    }

    @Test
    void itemOutsideCartHasZeroQuantityAndDefaultImage() {
        Item item = item(1, "Coffee", "12.50");
        item.setImagePath(null);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());
        ItemDto dto = service.getItem(1);
        assertThat(dto.count()).isZero();
        assertThat(dto.imgPath()).isEqualTo("images/default-image.svg");
    }

    @Test
    void missingItemFailsBeforeLookingUpCart() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getItem(99)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(cartItemRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidPageNumber(int page) {
        assertThatThrownBy(() -> service.getItems(null, ItemSort.NO, page, 5)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(itemRepository, cartItemRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidPageSize(int size) {
        assertThatThrownBy(() -> service.getItems(null, ItemSort.NO, 1, size)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(itemRepository, cartItemRepository);
    }
}
