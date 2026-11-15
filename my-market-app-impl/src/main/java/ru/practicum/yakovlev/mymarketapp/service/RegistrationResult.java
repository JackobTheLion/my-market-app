package ru.practicum.yakovlev.mymarketapp.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RegistrationResult {
    REGISTERED(null),
    INVALID_USERNAME("Логин должен содержать от 3 до 64 латинских букв, цифр или символов . _ -"),
    INVALID_PASSWORD("Пароль должен содержать от 8 до 72 символов"),
    USERNAME_TAKEN("Этот логин уже занят");

    private final String errorMessage;

}
