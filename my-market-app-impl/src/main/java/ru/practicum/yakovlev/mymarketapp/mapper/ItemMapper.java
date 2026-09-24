package ru.practicum.yakovlev.mymarketapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class ItemMapper {

    @Value("${application.images.default-image}")
    protected String defaultImage;

    @Mapping(target = "imgPath", source = "imagePath", qualifiedByName = "imageUrl")
    @Mapping(target = "count", constant = "0")
    public abstract ItemDto toDto(Item item);

    @Mapping(target = "imgPath", source = "item.imagePath", qualifiedByName = "imageUrl")
    @Mapping(target = "count", source = "count")
    public abstract ItemDto toDto(Item item, int count);

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "title", source = "item.title")
    @Mapping(target = "description", source = "item.description")
    @Mapping(target = "imgPath", source = "item.imagePath", qualifiedByName = "imageUrl")
    @Mapping(target = "price", source = "item.price")
    @Mapping(target = "count", source = "quantity")
    public abstract ItemDto toDto(CartItem cartItem);

    @Named("imageUrl")
    public String imageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            imagePath = defaultImage;
        }
        // Templates add the leading slash; also accept the legacy images/ prefix.
        String path = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
        return path.startsWith("images/") ? path : "images/" + path;
    }

}
