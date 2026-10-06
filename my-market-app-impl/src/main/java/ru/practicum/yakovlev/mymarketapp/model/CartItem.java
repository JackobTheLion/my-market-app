package ru.practicum.yakovlev.mymarketapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Table("cart_items")
public class CartItem {

    @Id
    @Column("item_id")
    private Long itemId;

    @Transient
    private Item item;

    @Column("quantity")
    private int quantity;

    public CartItem(Item item, int quantity) {
        this.item = item;
        this.itemId = item.getId();
        setQuantity(quantity);
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.quantity = quantity;
    }

    public void increment() {
        quantity = Math.incrementExact(quantity);
    }

    public void decrement() {
        if (quantity <= 1) {
            throw new IllegalStateException("Quantity cannot be decremented below one");
        }
        quantity--;
    }
}
