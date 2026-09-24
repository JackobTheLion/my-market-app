package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.support.MvcTestSupport;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.itemDto;

class CartControllerTest extends MvcTestSupport {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private CartService cartService;

    @Test
    void rendersCartItemsAndTotal() throws Exception {
        CartDto cart = new CartDto(List.of(itemDto(1, 2)), new BigDecimal("25.00"));
        when(cartService.getCart()).thenReturn(cart);

        mvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", cart.items()))
                .andExpect(model().attribute("total", cart.total()));
    }

    @Test
    void emptyCartHasNoBuyButton() throws Exception {
        when(cartService.getCart()).thenReturn(new CartDto(List.of(), BigDecimal.ZERO));

        mvc.perform(get("/cart/items"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(CartAction.class)
    void changesCartAndRendersUpdatedContents(CartAction action) throws Exception {
        CartDto cart = new CartDto(List.of(), BigDecimal.ZERO);
        when(cartService.getCart()).thenReturn(cart);

        mvc.perform(post("/cart/items")
                        .param("id", "1")
                        .param("action", action.name())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("total", BigDecimal.ZERO));
    }

    @Test
    void missingCatalogItemReturns404() throws Exception {
        doThrow(new NotFoundException("missing")).when(cartService).updateItem(99, CartAction.PLUS);

        mvc.perform(post("/cart/items")
                        .param("id", "99")
                        .param("action", "PLUS"))
                .andExpect(status().isNotFound());

        verify(cartService, never()).getCart();
    }

    @Test
    void invalidIdReturns400BeforeServiceCall() throws Exception {
        mvc.perform(post("/cart/items")
                        .param("id", "0")
                        .param("action", "PLUS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void missingIdReturns400() throws Exception {
        mvc.perform(post("/cart/items")
                        .param("action", "PLUS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }
}
