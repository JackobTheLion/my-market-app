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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.PagingDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.ItemMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {
    @Mock
    private ItemRepository items;
    @Mock
    private CartItemRepository cart;

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        ItemMapper mapper = Mappers.getMapper(ItemMapper.class);
        ReflectionTestUtils.setField(mapper, "defaultImage", "default-image.svg");
        service = new ItemServiceImpl(items, cart, mapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t\n"})
    void blankSearchReturnsEmptyCatalog(String search) {
        when(items.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("", "", PageRequest.of(0, 10, ItemSort.NO.getSort()))).thenReturn(Flux.empty());
        when(items.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("", "")).thenReturn(Mono.just(0L));
        StepVerifier.create(service.getItems(search, ItemSort.NO, 1, 10))
                .assertNext(page -> {
                    assertThat(page.items()).isEmpty();
                    assertThat(page.paging()).isEqualTo(new PagingDto(10, 1, false, false));
                })
                .verifyComplete();
        verifyNoInteractions(cart);
    }

    @Test
    void trimsSearchAndIncludesCartCountsAndPaging() {
        PageRequest request = PageRequest.of(1, 10, ItemSort.PRICE.getSort());
        Item coffee = item(1, "Coffee", "12.50");
        when(items.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("Coffee", "Coffee", request)).thenReturn(Flux.just(coffee));
        when(items.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("Coffee", "Coffee")).thenReturn(Mono.just(25L));
        when(cart.findAllById(List.of(1L))).thenReturn(Flux.just(new CartItem(coffee, 4)));
        StepVerifier.create(service.getItems("  Coffee  ", ItemSort.PRICE, 2, 10))
                .assertNext(page -> {
                    assertThat(page.paging()).isEqualTo(new PagingDto(10, 2, true, true));
                    assertThat(page.items().getFirst().count()).isEqualTo(4);
                })
                .verifyComplete();
    }

    @Test
    void outsideCartUsesZeroCountAndDefaultImage() {
        Item coffee = item(1, "Coffee", "12.50");
        coffee.setImagePath(null);
        when(items.findById(1L)).thenReturn(Mono.just(coffee));
        when(cart.findById(1L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getItem(1))
                .assertNext(dto -> {
                    assertThat(dto.count()).isZero();
                    assertThat(dto.imgPath()).isEqualTo("images/default-image.svg");
                })
                .verifyComplete();
    }

    @Test
    void detailsIncludeCartQuantity() {
        Item coffee = item(1, "Coffee", "12.50");
        when(items.findById(1L)).thenReturn(Mono.just(coffee));
        when(cart.findById(1L)).thenReturn(Mono.just(new CartItem(coffee, 3)));
        StepVerifier.create(service.getItem(1))
                .assertNext(dto -> assertThat(dto.count()).isEqualTo(3))
                .verifyComplete();
    }

    @Test
    void missingItemEmitsNotFound() {
        when(items.findById(99L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getItem(99))
                .expectError(NotFoundException.class)
                .verify();
        verifyNoInteractions(cart);
    }
}
