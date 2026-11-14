package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.controller.OrdersControllerApi;
import ru.practicum.yakovlev.mymarketapp.security.CustomerPrincipal;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;

@Controller
@RequiredArgsConstructor
public class OrdersController implements OrdersControllerApi {
    private final OrderService orderService;

    @Override
    public Mono<String> getOrders(Model model, CustomerPrincipal customer) {
        return orderService.getOrders(customer.getUserId()).map(page -> {
            model.addAttribute("orders", page.orders());
            model.addAttribute("total", page.totalSum());
            return "orders";
        });
    }

    @Override
    public Mono<String> getOrder(long id, boolean newOrder, Model model, CustomerPrincipal customer) {
        return orderService.getOrder(customer.getUserId(), id).map(order -> {
            model.addAttribute("order", order);
            model.addAttribute("newOrder", newOrder);
            return "order";
        });
    }
}
