package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.yakovlev.mymarketapp.exception.EmptyCartException;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;
import ru.practicum.yakovlev.mymarketapp.support.MvcTestSupport;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuyControllerTest extends MvcTestSupport {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private OrderService orderService;

    @Test
    void purchaseRedirectsToCreatedOrder() throws Exception {
        when(orderService.createOrder()).thenReturn(42L);
        mvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/42?newOrder=true"));
        verify(orderService).createOrder();
    }

    @Test
    void emptyCartReturns400() throws Exception {
        when(orderService.createOrder()).thenThrow(new EmptyCartException());
        mvc.perform(post("/buy"))
                .andExpect(status().isBadRequest());
    }

}
