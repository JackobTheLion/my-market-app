package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;
import ru.practicum.yakovlev.mymarketapp.dto.PagingDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;
import ru.practicum.yakovlev.mymarketapp.support.MvcTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.itemDto;

class ItemsControllerTest extends MvcTestSupport {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CartService cartService;

    @Test
    void rendersCatalogUsingDefaultParameters() throws Exception {
        ItemPageDto page = new ItemPageDto(List.of(itemDto(1, 0)), new PagingDto(10, 1, false, false));
        when(itemService.getItems(null, ItemSort.NO, 1, 10)).thenReturn(page);
        MvcResult result = mvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("sort", "NO"))
                .andExpect(model().attribute("paging", page.paging()))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Coffee")))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<List<ItemDto>> rows = (List<List<ItemDto>>) result.getModelAndView().getModel().get("items");
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).extracting(ItemDto::id).containsExactly(1L, -1L, -1L);
    }

    @Test
    void preparesCatalogRowsForView() throws Exception {
        List<ItemDto> items = List.of(itemDto(1, 0), itemDto(2, 0), itemDto(3, 0), itemDto(4, 0));
        when(itemService.getItems(null, ItemSort.NO, 1, 10))
                .thenReturn(new ItemPageDto(items, new PagingDto(10, 1, false, false)));

        MvcResult result = mvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<List<ItemDto>> rows = (List<List<ItemDto>>) result.getModelAndView().getModel().get("items");
        assertThat(rows).hasSize(2).allSatisfy(row -> assertThat(row).hasSize(3));
        assertThat(rows.getFirst()).extracting(ItemDto::id).containsExactly(1L, 2L, 3L);
        assertThat(rows.getLast()).extracting(ItemDto::id).containsExactly(4L, -1L, -1L);
    }

    @Test
    void passesSearchSortingAndPagingToService() throws Exception {
        ItemPageDto page = new ItemPageDto(List.of(), new PagingDto(20, 3, true, false));
        when(itemService.getItems("tea", ItemSort.PRICE, 3, 20)).thenReturn(page);
        mvc.perform(get("/items")
                        .param("search", "tea")
                        .param("sort", "PRICE")
                        .param("pageNumber", "3")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "tea"))
                .andExpect(model().attribute("sort", "PRICE"));
        verify(itemService).getItems("tea", ItemSort.PRICE, 3, 20);
    }

    @Test
    void catalogUpdatePreservesFiltersInRedirect() throws Exception {
        mvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS")
                        .param("search", "tea")
                        .param("sort", "ALPHA")
                        .param("pageNumber", "2")
                        .param("pageSize", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=tea&sort=ALPHA&pageNumber=2&pageSize=10"));
        verify(cartService).updateItem(1, CartAction.PLUS);
    }

    @Test
    void catalogUpdateWithoutSearchUsesDefaults() throws Exception {
        mvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "MINUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?sort=NO&pageNumber=1&pageSize=10"));
        verify(cartService).updateItem(1, CartAction.MINUS);
    }

    @Test
    void rendersItemDetails() throws Exception {
        ItemDto item = itemDto(1, 2);
        when(itemService.getItem(1)).thenReturn(item);
        mvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attribute("item", item));
    }

    @Test
    void itemUpdateRendersNewQuantity() throws Exception {
        ItemDto item = itemDto(1, 3);
        when(itemService.getItem(1)).thenReturn(item);
        mvc.perform(post("/items/1")
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attribute("item", item));
        InOrder calls = inOrder(cartService, itemService);
        calls.verify(cartService).updateItem(1, CartAction.PLUS);
        calls.verify(itemService).getItem(1);
    }

    @Test
    void missingItemReturns404() throws Exception {
        when(itemService.getItem(99)).thenThrow(new NotFoundException("missing"));
        mvc.perform(get("/items/99"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"pageNumber,0", "pageNumber,-1", "pageSize,0", "pageSize,-1", "pageSize,101",
            "pageSize,100000", "sort,UNKNOWN", "pageNumber,text"})
    void rejectsInvalidCatalogParameters(String parameter, String value) throws Exception {
        mvc.perform(get("/items")
                        .param(parameter, value))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(itemService, cartService);
    }

    @ParameterizedTest
    @CsvSource({"id,0", "id,-1", "action,UNKNOWN"})
    void rejectsInvalidCatalogUpdate(String parameter, String value) throws Exception {
        MockHttpServletRequestBuilder request = post("/items")
                .param("id", parameter.equals("id") ? value : "1")
                .param("action", parameter.equals("action") ? value : "PLUS");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cartService, itemService);
    }

    @Test
    void missingActionReturns400() throws Exception {
        mvc.perform(post("/items/1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cartService, itemService);
    }

    @Test
    void nonPositivePathIdReturns400() throws Exception {
        mvc.perform(get("/items/0"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(itemService);
    }
}
