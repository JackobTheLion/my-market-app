package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import ru.practicum.yakovlev.mymarketapp.api.controller.OrdersControllerApi;
import ru.practicum.yakovlev.mymarketapp.dto.OrdersPageDto;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;

@Controller
@RequiredArgsConstructor
public class OrdersController implements OrdersControllerApi {

    private final OrderService orderService;

    @Override
    public String getOrders(Model model) {
        OrdersPageDto page = orderService.getOrders();
        model.addAttribute("orders", page.orders());
        model.addAttribute("total", page.totalSum());
        return "orders";
    }

    @Override
    public String getOrder(long id, boolean newOrder, Model model) {
        model.addAttribute("order", orderService.getOrder(id));
        model.addAttribute("newOrder", newOrder);
        return "order";
    }
}
