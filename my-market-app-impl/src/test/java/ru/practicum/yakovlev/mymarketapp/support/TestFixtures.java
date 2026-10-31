package ru.practicum.yakovlev.mymarketapp.support;

import ru.practicum.yakovlev.mymarketapp.dto.ItemDto;
import ru.practicum.yakovlev.mymarketapp.model.Item;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;

public final class TestFixtures {
    private TestFixtures() {
    }

    public static Item item(String title, String price) {
        return new Item(title, "Description of " + title, "demo/photo.jpg", new BigDecimal(price));
    }

    public static Item item(long id, String title, String price) {
        Item item = item(title, price);
        item.setId(id);
        return item;
    }

    public static ItemDto itemDto(long id, int count) {
        return new ItemDto(id, "Coffee", "Arabica", "images/demo/coffee.jpg", new BigDecimal("12.50"), count);
    }

    public static byte[] imageBytes(String format) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output);
            return output.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
}
