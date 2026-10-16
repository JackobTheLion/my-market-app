package ru.practicum.yakovlev.mymarketapp.api.controller;

import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(ApiConstants.BUY_PATH)
public interface BuyControllerApi {

    @PostMapping
    Mono<String> buy();
}
