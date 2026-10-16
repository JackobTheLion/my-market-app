package ru.practicum.yakovlev.mymarketapp.controller;

import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.controller.HomeControllerApi;

@Controller
public class HomeController implements HomeControllerApi {

    @Override
    public Mono<String> redirectToItems() {
        return Mono.just("redirect:/items");
    }
}
