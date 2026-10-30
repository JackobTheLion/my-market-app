package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.controller.ItemCreateControllerApi;
import ru.practicum.yakovlev.mymarketapp.api.dto.ItemCreateRequest;
import ru.practicum.yakovlev.mymarketapp.exception.ItemCreationException;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;

@Controller
@RequiredArgsConstructor
public class ItemCreateController implements ItemCreateControllerApi {
    private final ItemService itemService;

    @Override
    public String getCreateForm(Model model) {
        model.addAttribute("itemForm", new ItemCreateRequest());
        return "item-create";
    }

    @Override
    public Mono<String> createItem(@ModelAttribute("itemForm") ItemCreateRequest request, BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            return Mono.just("item-create");
        }
        return itemService.createItem(request)
                .map(id -> "redirect:/items/" + id)
                .onErrorMap(error -> new ItemCreationException(request, error));
    }
}
