package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderRepository;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class MarketMvcIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Test
    void catalogCartPurchaseAndHistoryWorkThroughAllLayers() throws Exception {
        Long id = itemRepository.saveAndFlush(item("Coffee", "12.50")).getId();
        mvc.perform(get("/items")
                        .param("search", "coffee"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Coffee")));
        mvc.perform(post("/items/{id}", id)
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"));
        mvc.perform(post("/cart/items")
                        .param("id", id.toString())
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("total", new java.math.BigDecimal("25.00")));

        assertThat(cartItemRepository.findById(id).orElseThrow().getQuantity())
                .isEqualTo(2);
        MvcResult result = mvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection()).andReturn();
        OrderDto order = orderService.getOrders().orders().getFirst();

        assertThat(result.getResponse().getRedirectedUrl())
                .isEqualTo("/orders/" + order.id() + "?newOrder=true");
        assertThat(order.totalSum())
                .isEqualByComparingTo("25.00");

        assertThat(cartItemRepository.count()).isZero();

        mvc.perform(get(result.getResponse().getRedirectedUrl()))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("newOrder", true));
        mvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", java.util.List.of(order)))
                .andExpect(model().attribute("total", new BigDecimal("25.00")));
    }

    @Test
    void emptyPurchaseReturns400AndDoesNotPersistAnything() throws Exception {
        mvc.perform(post("/buy"))
                .andExpect(status().isBadRequest());
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void nonexistentItemAndOrderReturn404() throws Exception {
        mvc.perform(get("/items/999"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/cart/items")
                        .param("id", "999")
                        .param("action", "PLUS"))
                .andExpect(status().isNotFound());
        assertThat(cartItemRepository.count()).isZero();
    }

    @Test
    void invalidUpdateDoesNotChangeDatabase() throws Exception {
        Long id = itemRepository.saveAndFlush(item("Coffee", "12.50")).getId();
        mvc.perform(post("/cart/items")
                        .param("id", id.toString())
                        .param("action", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        assertThat(cartItemRepository.count()).isZero();
    }
}
