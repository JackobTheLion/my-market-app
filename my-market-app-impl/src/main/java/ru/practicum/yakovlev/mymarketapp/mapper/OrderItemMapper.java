package ru.practicum.yakovlev.mymarketapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.yakovlev.mymarketapp.dto.OrderItemDto;
import ru.practicum.yakovlev.mymarketapp.model.OrderItem;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemMapper {

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "count", source = "quantity")
    OrderItemDto toDto(OrderItem orderItem);
}
