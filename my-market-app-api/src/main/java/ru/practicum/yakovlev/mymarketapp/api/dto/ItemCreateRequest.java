package ru.practicum.yakovlev.mymarketapp.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCreateRequest {
    @NotBlank(message = "Укажите название товара")
    @Size(max = 255, message = "Название должно содержать не более 255 символов")
    private String title = "";

    @NotNull(message = "Укажите описание товара")
    private String description = "";

    @NotNull(message = "Укажите цену")
    @DecimalMin(value = "0.01", message = "Цена должна быть не меньше 0,01 руб.")
    @Digits(integer = 17, fraction = 2, message = "Допускается до 17 цифр до запятой и 2 после")
    private BigDecimal price;

    @NotNull(message = "Выберите изображение товара")
    private FilePart image;
}
