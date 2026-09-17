package ru.practicum.yakovlev.mymarketapp.api.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;

@RequestMapping(ApiConstants.CART_PATH)
public interface CartControllerApi {

    @GetMapping("/items")
    String getCart(Model model);

    @PostMapping("/items")
    String updateItemInCart(
            @RequestParam("id") @Positive long id,
            @RequestParam("action") @NotNull CartAction action,
            Model model
    );
}
