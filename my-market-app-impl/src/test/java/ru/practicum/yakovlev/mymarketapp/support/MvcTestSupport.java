package ru.practicum.yakovlev.mymarketapp.support;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@WebMvcTest({HomeController.class, ItemsController.class, CartController.class,
        BuyController.class, OrdersController.class, ImageController.class})
@ActiveProfiles("test")
@MockitoBean(types = {ItemService.class, CartService.class, OrderService.class, ImageService.class})
public abstract class MvcTestSupport {
}
