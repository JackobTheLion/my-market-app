package ru.practicum.yakovlev.mymarketapp.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

public abstract class DatabaseTestSupport {
    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        sql("DELETE FROM order_items")
                .then(sql("DELETE FROM orders"))
                .then(sql("DELETE FROM cart_items"))
                .then(sql("DELETE FROM items"))
                .block();
    }

    protected Mono<Void> sql(String statement) {
        return databaseClient.sql(statement)
                .fetch()
                .rowsUpdated()
                .then();
    }
}
