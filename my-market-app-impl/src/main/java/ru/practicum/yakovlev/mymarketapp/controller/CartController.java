package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.controller.CartControllerApi;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.service.CartService;

@Controller
@RequiredArgsConstructor
public class CartController implements CartControllerApi {
    private final CartService cartService;

    @Override
    public Mono<String> getCart(Model model) {
        return cartService.getCart()
                .map(cart -> {
                    model.addAttribute("items", cart.items());
                    model.addAttribute("total", cart.total());
                    return "cart";
                });
    }

    @Override
    public Mono<String> updateItemInCart(long id, CartAction action, Model model) {
        return cartService.updateItem(id, action)
                .thenReturn("redirect:/cart/items");
    }
}
