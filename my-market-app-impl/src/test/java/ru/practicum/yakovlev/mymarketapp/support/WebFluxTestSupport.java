package ru.practicum.yakovlev.mymarketapp.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.practicum.yakovlev.mymarketapp.config.CustomerDetailsConfiguration;
import ru.practicum.yakovlev.mymarketapp.config.SecurityConfiguration;
import ru.practicum.yakovlev.mymarketapp.controller.BuyController;
import ru.practicum.yakovlev.mymarketapp.controller.CartController;
import ru.practicum.yakovlev.mymarketapp.controller.HomeController;
import ru.practicum.yakovlev.mymarketapp.controller.ImageController;
import ru.practicum.yakovlev.mymarketapp.controller.ItemCreateController;
import ru.practicum.yakovlev.mymarketapp.controller.ItemsController;
import ru.practicum.yakovlev.mymarketapp.controller.OrdersController;
import ru.practicum.yakovlev.mymarketapp.controller.RegistrationController;
import ru.practicum.yakovlev.mymarketapp.repository.CustomerRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ImageService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;
import ru.practicum.yakovlev.mymarketapp.service.impl.CustomerServiceImpl;
import ru.practicum.yakovlev.mymarketapp.payment.PaymentGateway;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest({HomeController.class, ItemsController.class, CartController.class,
        BuyController.class, OrdersController.class, ImageController.class, ItemCreateController.class, RegistrationController.class})
@Import({SecurityConfiguration.class, CustomerServiceImpl.class, CustomerDetailsConfiguration.class})
@ActiveProfiles("test")
public abstract class WebFluxTestSupport {
    @Autowired
    protected WebTestClient webTestClient;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @MockitoBean
    protected CustomerRepository customers;
    protected WebTestClient client;

    @BeforeEach
    void configureCsrf() {
        client = webTestClient.mutateWith(csrf());
    }

    @MockitoBean
    protected PaymentGateway paymentGateway;
    @MockitoBean
    protected ItemService itemService;
    @MockitoBean
    protected CartService cartService;
    @MockitoBean
    protected OrderService orderService;
    @MockitoBean
    protected ImageService imageService;
}
