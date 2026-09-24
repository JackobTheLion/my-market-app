package ru.practicum.yakovlev.mymarketapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.practicum.yakovlev.mymarketapp.dto.CartDto;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;

import java.math.BigDecimal;
import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = ItemMapper.class
)
public interface CartMapper {

    List<ItemDto> toItemDtos(List<CartItem> cartItems);

    default CartDto toDto(List<CartItem> cartItems) {
        if (cartItems == null) {
            return null;
        }

        List<ItemDto> items = toItemDtos(cartItems);
        BigDecimal total = cartItems.stream()
                .map(cartItem -> cartItem.getItem().getPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(items, total);
    }
}
