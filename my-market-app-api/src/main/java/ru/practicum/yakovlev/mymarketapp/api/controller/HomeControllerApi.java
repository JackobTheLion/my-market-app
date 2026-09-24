package ru.practicum.yakovlev.mymarketapp.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(ApiConstants.ROOT_PATH)
public interface HomeControllerApi {

    @GetMapping
    String redirectToItems();
}
