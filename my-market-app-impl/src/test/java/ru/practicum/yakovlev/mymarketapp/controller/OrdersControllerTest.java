package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.dto.OrderItemDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;
import ru.practicum.yakovlev.mymarketapp.support.MvcTestSupport;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrdersControllerTest extends MvcTestSupport {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private OrderService orderService;

    @Test
    void rendersHistory() throws Exception {
        List<OrderDto> orders = List.of(new OrderDto(1, List.of(new OrderItemDto(2, "Coffee", new BigDecimal("12.50"), 2)), new BigDecimal("25.00")));

        when(orderService.getOrders()).thenReturn(orders);

        mvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", orders));
    }

    @Test
    void rendersOrderWithDefaultNewOrderFlag() throws Exception {
        OrderDto order = new OrderDto(1, List.of(), BigDecimal.ZERO);

        when(orderService.getOrder(1)).thenReturn(order);

        mvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("newOrder", false));
    }

    @Test
    void missingOrderReturns404() throws Exception {
        when(orderService.getOrder(99)).thenThrow(new NotFoundException("missing"));

        mvc.perform(get("/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidOrderIdReturns400() throws Exception {
        mvc.perform(get("/orders/0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }
}
