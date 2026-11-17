package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.controller.CartControllerApi;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException.Reason;
import ru.practicum.yakovlev.mymarketapp.payment.PaymentGateway;
import ru.practicum.yakovlev.mymarketapp.security.CustomerPrincipal;
import ru.practicum.yakovlev.mymarketapp.service.CartService;

@Controller
@RequiredArgsConstructor
public class CartController implements CartControllerApi {
    private final CartService cartService;
    private final PaymentGateway paymentGateway;

    @Override
    public Mono<String> getCart(String paymentError, Model model, CustomerPrincipal customer) {
        long userId = customer.getUserId();
        if (paymentError != null) {
            model.addAttribute("checkoutErrorMessage", Reason.fromCode(paymentError).getMessage());
        }
        return cartService.getCart(userId).flatMap(cart -> {
            model.addAttribute("items", cart.items());
            model.addAttribute("total", cart.total());
            model.addAttribute("canBuy", false);
            if (cart.items().isEmpty()) {
                return Mono.just("cart");
            }
            return paymentGateway.getBalance(userId).map(balance -> {
                model.addAttribute("balance", balance);
                boolean enough = balance.compareTo(cart.total()) >= 0;
                model.addAttribute("canBuy", enough);
                if (!enough) {
                    model.addAttribute("paymentMessage", Reason.INSUFFICIENT_FUNDS.getMessage());
                }
                return "cart";
            }).onErrorResume(PaymentException.class, error -> {
                model.addAttribute("paymentMessage", error.getMessage());
                return Mono.just("cart");
            });
        });
    }

    @Override
    public Mono<String> updateItemInCart(long id, CartAction action, Model model, CustomerPrincipal customer) {
        return cartService.updateItem(customer.getUserId(), id, action)
                .thenReturn("redirect:/cart/items");
    }
}
