package ru.practicum.yakovlev.mymarketapp.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.dto.ItemCreateRequest;

@RequestMapping(ApiConstants.ITEM_CREATE_PATH)
public interface ItemCreateControllerApi {

    @GetMapping
    String getCreateForm(Model model);

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<String> createItem(@Valid @ModelAttribute("itemForm") ItemCreateRequest request,
                             BindingResult errors, Model model);
}
