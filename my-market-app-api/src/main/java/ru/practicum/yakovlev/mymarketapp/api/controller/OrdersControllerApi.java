package ru.practicum.yakovlev.mymarketapp.api.controller;

import jakarta.validation.constraints.Positive;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(ApiConstants.ORDERS_PATH)
public interface OrdersControllerApi {

    @GetMapping
    String getOrders(Model model);

    @GetMapping("/{id}")
    String getOrder(
            @PathVariable("id") @Positive long id,
            @RequestParam(name = "newOrder", required = false, defaultValue = "false") boolean newOrder,
            Model model
    );

}
