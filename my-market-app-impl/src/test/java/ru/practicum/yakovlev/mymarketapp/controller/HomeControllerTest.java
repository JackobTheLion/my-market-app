package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.yakovlev.mymarketapp.support.MvcTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeControllerTest extends MvcTestSupport {
    @Autowired
    private MockMvc mvc;

    @Test
    void homeRedirectsToCatalog() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }
}
