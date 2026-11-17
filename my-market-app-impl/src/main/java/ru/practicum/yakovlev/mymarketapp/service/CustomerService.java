package ru.practicum.yakovlev.mymarketapp.service;

import reactor.core.publisher.Mono;

public interface CustomerService {

    Mono<RegistrationResult> register(String username, String password);
}
