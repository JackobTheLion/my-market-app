package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.service.CustomerService;

@Controller
@RequiredArgsConstructor
public class RegistrationController {
    private final CustomerService customerService;

    @GetMapping("/register")
    public String form() {
        return "register";
    }

    @PostMapping("/register")
    public Mono<String> register(ServerWebExchange exchange, Model model) {
        return exchange.getFormData().flatMap(form -> {
            String username = form.getFirst("username");
            String password = form.getFirst("password");
            if (username == null || !username.matches("[A-Za-z0-9._-]{3,64}")) {
                model.addAttribute("error", "Логин должен содержать от 3 до 64 латинских букв, цифр или символов . _ -");
                return Mono.just("register");
            }
            model.addAttribute("username", username);
            if (!isValidPassword(password)) {
                model.addAttribute("error", "Пароль должен содержать от 8 до 72 символов");
                return Mono.just("register");
            }
            return customerService.register(username, password)
                    .map(registered -> {
                        if (registered) {
                            return "redirect:/login";
                        }
                        model.addAttribute("error", "Этот логин уже занят");
                        return "register";
                    });
        });
    }

    private static boolean isValidPassword(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        int length = password.codePointCount(0, password.length());
        return length >= 8 && length <= 72;
    }
}
