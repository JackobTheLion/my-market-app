package ru.practicum.yakovlev.mymarketapp.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(ApiConstants.BUY_PATH)
public interface BuyControllerApi {

    @PostMapping
    String buy();
}
