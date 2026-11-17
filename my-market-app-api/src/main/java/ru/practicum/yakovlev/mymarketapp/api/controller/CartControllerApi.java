package ru.practicum.yakovlev.mymarketapp.api.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.security.CustomerPrincipal;

@RequestMapping(ApiConstants.CART_PATH)
public interface CartControllerApi {

    @GetMapping("/items")
    Mono<String> getCart(
            @RequestParam(name = "paymentError", required = false) String paymentError,
            Model model,
            @AuthenticationPrincipal CustomerPrincipal customer
    );

    @PostMapping("/items")
    Mono<String> updateItemInCart(
            @RequestParam("id") @Positive long id,
            @RequestParam("action") @NotNull CartAction action,
            Model model,
            @AuthenticationPrincipal CustomerPrincipal customer
    );
}
