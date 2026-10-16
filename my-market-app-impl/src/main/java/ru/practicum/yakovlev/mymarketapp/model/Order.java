package ru.practicum.yakovlev.mymarketapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Table("orders")
public class Order {

    @Id
    private Long id;

    @Column("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Transient
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(Item item, int quantity) {
        items.add(new OrderItem(this, item, quantity));
    }

    public BigDecimal getTotalSum() {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
