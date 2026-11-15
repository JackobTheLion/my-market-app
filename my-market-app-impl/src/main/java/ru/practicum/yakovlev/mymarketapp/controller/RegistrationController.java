package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.service.CustomerService;
import ru.practicum.yakovlev.mymarketapp.service.RegistrationResult;

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
            return customerService.register(username, form.getFirst("password"))
                    .map(result -> {
                        if (result == RegistrationResult.REGISTERED) {
                            return "redirect:/login";
                        }
                        model.addAttribute("error", result.getErrorMessage());
                        return "register";
                    });
        });
    }
}
