package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;
import ru.practicum.yakovlev.mymarketapp.dto.PagingDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class ItemServiceIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private ItemService itemService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemRepository itemRepository;

    private long coffeeId;

    @BeforeEach
    void seed() {
        Item coffee = item("Coffee", "12.50");
        coffee.setImagePath(null);
        coffeeId = itemRepository.saveAndFlush(coffee).getId();
        Item tea = item("Tea", "3.25");
        tea.setDescription("Coffee flavoured tea");
        itemRepository.saveAndFlush(tea);
        itemRepository.saveAndFlush(item("Mug", "7.00"));
        itemRepository.saveAndFlush(item("Cable", "4.00"));
    }

    @Test
    void searchesBothFieldsAndIncludesPersistedCartQuantity() {
        cartService.updateItem(coffeeId, CartAction.PLUS);
        cartService.updateItem(coffeeId, CartAction.PLUS);
        ItemPageDto page = itemService.getItems("  cOfFeE  ", ItemSort.ALPHA, 1, 5);

        assertThat(realItems(page)).extracting(ItemDto::title)
                .containsExactly("Coffee", "Tea");
        assertThat(realItems(page)).extracting(ItemDto::count)
                .containsExactly(2, 0);
        assertThat(realItems(page).getFirst().imgPath())
                .isEqualTo("images/default-image.svg");
        assertThat(page.items().getFirst())
                .hasSize(3);
        assertThat(page.items().getFirst().getLast())
                .isEqualTo(ItemDto.defaultItem());
    }

    @Test
    void sortsPricesAndReportsPageNavigation() {
        ItemPageDto first = itemService.getItems(null, ItemSort.PRICE, 1, 2);

        assertThat(realItems(first)).extracting(ItemDto::title)
                .containsExactly("Tea", "Cable");
        assertThat(first.paging())
                .isEqualTo(new PagingDto(2, 1, false, true));

        ItemPageDto second = itemService.getItems(null, ItemSort.PRICE, 2, 2);

        assertThat(realItems(second)).extracting(ItemDto::title)
                .containsExactly("Mug", "Coffee");
        assertThat(second.paging())
                .isEqualTo(new PagingDto(2, 2, true, false));
    }

    @Test
    void alphabeticCatalogPadsLastOfTwoRows() {
        ItemPageDto page = itemService.getItems(" ", ItemSort.ALPHA, 1, 5);

        assertThat(realItems(page)).extracting(ItemDto::title)
                .containsExactly("Cable", "Coffee", "Mug", "Tea");
        assertThat(page.items())
                .hasSize(2);
        assertThat(page.items().getLast()).extracting(ItemDto::id)
                .containsExactly(realItems(page).getLast().id(), -1L, -1L);
    }

    @Test
    void unknownSearchAndPagePastEndAreEmpty() {
        assertThat(itemService.getItems("missing", ItemSort.NO, 1, 5).items())
                .isEmpty();
        assertThat(itemService.getItems(null, ItemSort.PRICE, 10, 5).items())
                .isEmpty();
    }

    @Test
    void loadsDetailsAndRejectsMissingItem() {
        assertThat(itemService.getItem(coffeeId).title())
                .isEqualTo("Coffee");
        assertThat(itemService.getItem(coffeeId).count())
                .isZero();
        assertThatThrownBy(() -> itemService.getItem(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
    }

    private List<ItemDto> realItems(ItemPageDto page) {
        return page.items().stream().flatMap(List::stream).filter(item -> item.id() != -1).toList();
    }
}
