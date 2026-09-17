package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.practicum.yakovlev.mymarketapp.api.controller.ItemsControllerApi;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;

@Controller
@RequiredArgsConstructor
public class ItemsController implements ItemsControllerApi {

    private final ItemService itemService;
    private final CartService cartService;

    @Override
    public String getItems(
            String search,
            ItemSort sort,
            int pageNumber,
            int pageSize,
            Model model
    ) {
        ItemPageDto page = itemService.getItems(search, sort, pageNumber, pageSize);

        model.addAttribute("items", page.items());
        model.addAttribute("search", search);
        model.addAttribute("sort", sort.name());
        model.addAttribute("paging", page.paging());
        return "items";
    }

    @Override
    public String updateItemInCart(
            long id,
            String search,
            ItemSort sort,
            int pageNumber,
            int pageSize,
            CartAction action,
            RedirectAttributes redirectAttributes
    ) {
        cartService.updateItem(id, action);

        if (search != null) {
            redirectAttributes.addAttribute("search", search);
        }
        redirectAttributes.addAttribute("sort", sort);
        redirectAttributes.addAttribute("pageNumber", pageNumber);
        redirectAttributes.addAttribute("pageSize", pageSize);

        return "redirect:/items";
    }

    @Override
    public String getItem(long id, Model model) {
        model.addAttribute("item", itemService.getItem(id));
        return "item";
    }

    @Override
    public String updateItemInCart(long id, CartAction action, Model model) {
        cartService.updateItem(id, action);
        return getItem(id, model);
    }

}
