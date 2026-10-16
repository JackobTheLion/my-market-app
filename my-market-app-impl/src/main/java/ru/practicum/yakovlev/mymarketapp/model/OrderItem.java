package ru.practicum.yakovlev.mymarketapp.model;

import lombok.AccessLevel;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("item_id")
    private Long itemId;

    @Column("title")
    private String title;

    @Column("price")
    private BigDecimal price;

    @Column("image_path")
    private String imagePath;

    @Column("quantity")
    private int quantity;

    public OrderItem(Order order, Item item, int quantity) {
        if (item.getPrice() == null || item.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.orderId = order.getId();
        this.itemId = item.getId();
        this.title = item.getTitle();
        this.price = item.getPrice();
        this.imagePath = item.getImagePath();
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
