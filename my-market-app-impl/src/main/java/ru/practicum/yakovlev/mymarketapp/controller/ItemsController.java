package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.controller.ItemsControllerApi;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.security.CustomerPrincipal;
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
    public Mono<String> getItems(
            String search,
            ItemSort sort,
            int pageNumber,
            int pageSize,
            Model model,
            CustomerPrincipal customer
    ) {
        Long customerId = customer == null ? null : customer.getUserId();
        model.addAttribute("authenticated", customerId != null);
        return itemService.getItems(customerId, search, sort, pageNumber, pageSize)
                .map(page -> {
                    model.addAttribute("items", prepareRows(page.items()));
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort.name());
                    model.addAttribute("paging", page.paging());
                    return "items";
                });
    }

    @Override
    public Mono<String> updateItemInCart(
            long id,
            String search,
            ItemSort sort,
            int pageNumber,
            int pageSize,
            CartAction action,
            Model model,
            CustomerPrincipal customer
    ) {
        String redirect = "redirect:/items?";
        if (search != null) {
            model.addAttribute("search", search);
            redirect += "search={search}&";
        }
        model.addAttribute("sort", sort);
        model.addAttribute("pageNumber", pageNumber);
        model.addAttribute("pageSize", pageSize);

        return cartService.updateItem(customer.getUserId(), id, action)
                .thenReturn(redirect + "sort={sort}&pageNumber={pageNumber}&pageSize={pageSize}");
    }

    @Override
    public Mono<String> getItem(long id, Model model, CustomerPrincipal customer) {
        Long customerId = customer == null ? null : customer.getUserId();
        model.addAttribute("authenticated", customerId != null);
        return itemService.getItem(customerId, id)
                .map(item -> {
                    model.addAttribute("item", item);
                    return "item";
                });
    }

    @Override
    public Mono<String> updateItemInCart(long id, CartAction action, Model model, CustomerPrincipal customer) {
        return cartService.updateItem(customer.getUserId(), id, action)
                .thenReturn("redirect:/items/" + id);
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
