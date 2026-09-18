package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.practicum.yakovlev.mymarketapp.api.controller.ItemsControllerApi;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;

import java.util.LinkedList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemsController implements ItemsControllerApi {

    private static final int ITEMS_PER_ROW = 3;

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

        model.addAttribute("items", prepareRows(page.items()));
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

    private List<List<ItemDto>> prepareRows(List<ItemDto> items) {
        List<List<ItemDto>> rows = new LinkedList<>();
        for (int offset = 0; offset < items.size(); offset += ITEMS_PER_ROW) {
            List<ItemDto> row = new LinkedList<>(items.subList(
                    offset,
                    Math.min(offset + ITEMS_PER_ROW, items.size())
            ));
            while (row.size() < ITEMS_PER_ROW) {
                row.add(ItemDto.defaultItem());
            }
            rows.add(List.copyOf(row));
        }
        return List.copyOf(rows);
    }

}
