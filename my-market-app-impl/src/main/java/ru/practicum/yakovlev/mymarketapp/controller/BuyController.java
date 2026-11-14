package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.controller.BuyControllerApi;
import ru.practicum.yakovlev.mymarketapp.security.CustomerPrincipal;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;

@Controller
@RequiredArgsConstructor
public class BuyController implements BuyControllerApi {

    private final OrderService orderService;

    @Override
    public Mono<String> buy(CustomerPrincipal customer) {
        return orderService.createOrder(customer.getUserId())
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }
}
