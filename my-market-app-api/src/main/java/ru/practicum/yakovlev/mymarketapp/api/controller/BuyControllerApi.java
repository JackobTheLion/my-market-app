package ru.practicum.yakovlev.mymarketapp.api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.security.CustomerPrincipal;

@RequestMapping(ApiConstants.BUY_PATH)
public interface BuyControllerApi {

    @PostMapping
    Mono<String> buy(@AuthenticationPrincipal CustomerPrincipal customer);
}
