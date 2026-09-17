package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import ru.practicum.yakovlev.mymarketapp.api.controller.CartControllerApi;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;
import ru.practicum.yakovlev.mymarketapp.service.CartService;

@Controller
@RequiredArgsConstructor
public class CartController implements CartControllerApi {

    private final CartService cartService;

    @Override
    public String getCart(Model model) {
        return renderCart(model);
    }

    @Override
    public String updateItemInCart(long id, CartAction action, Model model) {
        cartService.updateItem(id, action);
        return renderCart(model);
    }

    private String renderCart(Model model) {
        CartDto cart = cartService.getCart();
        model.addAttribute("items", cart.items());
        model.addAttribute("total", cart.total());
        return "cart";
    }
}
