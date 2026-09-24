package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import ru.practicum.yakovlev.mymarketapp.api.controller.BuyControllerApi;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;

@Controller
@RequiredArgsConstructor
public class BuyController implements BuyControllerApi {

    private final OrderService orderService;

    @Override
    public String buy() {
        long orderId = orderService.createOrder();
        return "redirect:/orders/" + orderId + "?newOrder=true";
    }
}
