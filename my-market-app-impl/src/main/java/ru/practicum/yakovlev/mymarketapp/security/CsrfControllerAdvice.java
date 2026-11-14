package ru.practicum.yakovlev.mymarketapp.security;

import org.springframework.security.web.reactive.result.view.CsrfRequestDataValueProcessor;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.controller.CartController;
import ru.practicum.yakovlev.mymarketapp.controller.ItemCreateController;
import ru.practicum.yakovlev.mymarketapp.controller.ItemsController;
import ru.practicum.yakovlev.mymarketapp.controller.RegistrationController;

@ControllerAdvice(assignableTypes = {RegistrationController.class, ItemsController.class,
        CartController.class, ItemCreateController.class})
public class CsrfControllerAdvice {
    @ModelAttribute
    public Mono<CsrfToken> csrfToken(ServerWebExchange exchange) {
        Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
        return token == null ? Mono.empty() : token.doOnNext(value ->
                exchange.getAttributes().put(CsrfRequestDataValueProcessor.DEFAULT_CSRF_ATTR_NAME, value));
    }
}
