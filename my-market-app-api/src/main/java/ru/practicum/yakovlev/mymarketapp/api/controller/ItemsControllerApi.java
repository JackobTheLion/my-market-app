package ru.practicum.yakovlev.mymarketapp.api.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;

@RequestMapping(ApiConstants.ITEMS_PATH)
public interface ItemsControllerApi {

    @GetMapping
    Mono<String> getItems(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", required = false, defaultValue = "NO") @NotNull ItemSort sort,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") @Positive int pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") @Positive @Max(100) int pageSize,
            Model model
    );

    @PostMapping
    Mono<String> updateItemInCart(
            @RequestParam("id") @Positive long id,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", required = false, defaultValue = "NO") ItemSort sort,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") @Positive int pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") @Positive @Max(100) int pageSize,
            @RequestParam("action") @NotNull CartAction action,
            Model model
    );

    @GetMapping("/{id}")
    Mono<String> getItem(@PathVariable("id") @Positive long id, Model model);

    @PostMapping("/{id}")
    Mono<String> updateItemInCart(
            @PathVariable("id") @Positive long id,
            @RequestParam("action") @NotNull CartAction action,
            Model model
    );
}
