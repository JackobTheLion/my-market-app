package ru.practicum.yakovlev.mymarketapp.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.practicum.yakovlev.mymarketapp.controller.BuyController;
import ru.practicum.yakovlev.mymarketapp.controller.CartController;
import ru.practicum.yakovlev.mymarketapp.controller.HomeController;
import ru.practicum.yakovlev.mymarketapp.controller.ImageController;
import ru.practicum.yakovlev.mymarketapp.controller.ItemsController;
import ru.practicum.yakovlev.mymarketapp.controller.OrdersController;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ImageService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;

@WebFluxTest({HomeController.class, ItemsController.class, CartController.class,
        BuyController.class, OrdersController.class, ImageController.class})
@ActiveProfiles("test")
public abstract class WebFluxTestSupport {
    @Autowired
    protected WebTestClient client;
    @MockitoBean
    protected ItemService itemService;
    @MockitoBean
    protected CartService cartService;
    @MockitoBean
    protected OrderService orderService;
    @MockitoBean
    protected ImageService imageService;
}
