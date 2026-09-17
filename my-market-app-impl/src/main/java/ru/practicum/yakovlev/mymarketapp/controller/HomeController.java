package ru.practicum.yakovlev.mymarketapp.controller;

import org.springframework.stereotype.Controller;
import ru.practicum.yakovlev.mymarketapp.api.controller.HomeControllerApi;

@Controller
public class HomeController implements HomeControllerApi {

    @Override
    public String redirectToItems() {
        return "redirect:/items";
    }
}
