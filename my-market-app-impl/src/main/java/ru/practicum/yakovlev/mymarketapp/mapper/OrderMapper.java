package ru.practicum.yakovlev.mymarketapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.practicum.yakovlev.mymarketapp.dto.OrderDto;
import ru.practicum.yakovlev.mymarketapp.model.Order;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = OrderItemMapper.class
)
public interface OrderMapper {

    OrderDto toDto(Order order);

    List<OrderDto> toDtos(List<Order> orders);
}
