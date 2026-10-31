package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.yakovlev.mymarketapp.api.dto.ItemCreateRequest;
import ru.practicum.yakovlev.mymarketapp.exception.ItemCreationException;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public String paymentFailed(PaymentException exception) {
        String location = UriComponentsBuilder.fromPath("/cart/items")
                .queryParam("paymentError", exception.getReason()).build().encode().toUriString();
        return "redirect:" + location;
    }

    @ExceptionHandler(DataBufferLimitException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String uploadLimitExceeded(Model model) {
        model.addAttribute("itemForm", new ItemCreateRequest());
        model.addAttribute("uploadError", "Размер изображения не должен превышать 5 МиБ");
        return "item-create";
    }

    @ExceptionHandler(ItemCreationException.class)
    public String itemCreationFailed(ItemCreationException exception, Model model) {
        BindingResult errors = new BeanPropertyBindingResult(exception.getRequest(), "itemForm");
        if (exception.getCause() instanceof ResponseStatusException statusException) {
            errors.rejectValue("image", "image.invalid", statusException.getStatusCode().value() == 413
                    ? "Размер изображения не должен превышать 5 МиБ"
                    : "Выберите корректное изображение PNG или JPEG");
        } else {
            log.warn("Cannot create product", exception.getCause());
            errors.reject("item.create.failed", "Не удалось добавить товар. Попробуйте ещё раз");
        }
        model.addAllAttributes(errors.getModel());
        return "item-create";
    }
}
