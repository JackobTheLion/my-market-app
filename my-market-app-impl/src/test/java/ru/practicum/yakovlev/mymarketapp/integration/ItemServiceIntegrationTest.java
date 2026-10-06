package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.PagingDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class ItemServiceIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private ItemService service;
    @Autowired
    private CartService cart;
    @Autowired
    private ItemRepository items;
    long coffeeId;

    @BeforeEach
    void seed() {
        Item coffee = item("Coffee", "12.50");
        coffee.setImagePath(null);
        coffeeId = items.save(coffee)
                .block()
                .getId();
        Item tea = item("Tea", "3.25");
        tea.setDescription("Coffee flavoured tea");
        items.save(tea)
                .then(items.save(item("Mug", "7.00")))
                .then(items.save(item("Cable", "4.00")))
                .block();
    }

    @Test
    void searchIncludesDescriptionAndCartQuantity() {
        cart.updateItem(coffeeId, CartAction.PLUS)
                .then(cart.updateItem(coffeeId, CartAction.PLUS))
                .block();
        StepVerifier.create(service.getItems("  cOfFeE  ", ItemSort.ALPHA, 1, 10))
                .assertNext(page -> {
                    assertThat(page.items())
                            .extracting(ItemDto::title)
                            .containsExactly("Coffee", "Tea");
                    assertThat(page.items())
                            .extracting(ItemDto::count)
                            .containsExactly(2, 0);
                    assertThat(page.items().getFirst().imgPath()).isEqualTo("images/default-image.svg");
                })
                .verifyComplete();
    }

    @Test
    void pricePagingReportsBothNavigationFlags() {
        StepVerifier.create(service.getItems(null, ItemSort.PRICE, 1, 3))
                .assertNext(page -> {
                    assertThat(page.items())
                            .extracting(ItemDto::title)
                            .containsExactly("Tea", "Cable", "Mug");
                    assertThat(page.paging()).isEqualTo(new PagingDto(3, 1, false, true));
                })
                .verifyComplete();
        StepVerifier.create(service.getItems(null, ItemSort.PRICE, 2, 3))
                .assertNext(page -> {
                    assertThat(page.items())
                            .extracting(ItemDto::title)
                            .containsExactly("Coffee");
                    assertThat(page.paging()).isEqualTo(new PagingDto(3, 2, true, false));
                })
                .verifyComplete();
    }

    @Test
    void alphabeticCatalogContainsOnlyRealItems() {
        StepVerifier.create(service.getItems(" ", ItemSort.ALPHA, 1, 10))
                .assertNext(page ->
                        assertThat(page.items())
                                .extracting(ItemDto::title)
                                .containsExactly("Cable", "Coffee", "Mug", "Tea"))
                .verifyComplete();
    }

    @Test
    void emptySearchAndPagePastEndReturnEmptyLists() {
        StepVerifier.create(service.getItems("missing", ItemSort.NO, 1, 10))
                .assertNext(page -> assertThat(page.items()).isEmpty())
                .verifyComplete();
        StepVerifier.create(service.getItems(null, ItemSort.PRICE, 10, 10))
                .assertNext(page -> {
                    assertThat(page.items()).isEmpty();
                    assertThat(page.paging().hasNext()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void detailsAndNotFoundWorkReactively() {
        StepVerifier.create(service.getItem(coffeeId))
                .assertNext(dto -> {
                    assertThat(dto.title()).isEqualTo("Coffee");
                    assertThat(dto.count()).isZero();
                })
                .verifyComplete();
        StepVerifier.create(service.getItem(Long.MAX_VALUE))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void priceSortingIncludesAllItemsWithEqualPrices() {
        Item first = items.save(item("Same", "100"))
                .block();
        Item second = items.save(item("Same", "100"))
                .block();
        StepVerifier.create(service.getItems("Same", ItemSort.PRICE, 1, 10))
                .assertNext(page -> assertThat(page.items())
                        .extracting(ItemDto::id)
                        .containsExactlyInAnyOrder(first.getId(), second.getId()))
                .verifyComplete();
    }
}
