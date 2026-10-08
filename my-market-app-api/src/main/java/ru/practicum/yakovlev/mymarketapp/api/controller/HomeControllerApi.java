package ru.practicum.yakovlev.mymarketapp.api.controller;

import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(ApiConstants.ROOT_PATH)
public interface HomeControllerApi {

    @GetMapping
    Mono<String> redirectToItems();
}
