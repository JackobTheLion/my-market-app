package ru.practicum.yakovlev.mymarketapp.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepositoryCustom;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class CartItemRepositoryCustomImpl implements CartItemRepositoryCustom {
    private final DatabaseClient databaseClient;

    @Override
    public Flux<CartItem> findAllWithItems() {
        return findAllWithItems(false);
    }

    @Override
    public Flux<CartItem> findAllWithItemsForUpdate() {
        return findAllWithItems(true);
    }

    private Flux<CartItem> findAllWithItems(boolean forUpdate) {
        return databaseClient.sql("""
                        SELECT cart.item_id, cart.quantity,
                               item.title, item.description, item.image_path, item.price
                        FROM cart_items cart
                        JOIN items item ON item.id = cart.item_id
                        ORDER BY cart.item_id
                        """ + (forUpdate ? " FOR UPDATE OF cart" : ""))
                .map((row, metadata) -> {
                    Item item = new Item(row.get("title", String.class), row.get("description", String.class),
                            row.get("image_path", String.class), row.get("price", BigDecimal.class));
                    item.setId(row.get("item_id", Long.class));
                    return new CartItem(item, row.get("quantity", Integer.class));
                })
                .all();
    }
}
