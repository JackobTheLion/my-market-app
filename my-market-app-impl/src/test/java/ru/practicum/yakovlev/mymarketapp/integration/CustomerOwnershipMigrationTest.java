package ru.practicum.yakovlev.mymarketapp.integration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerOwnershipMigrationTest extends IntegrationTestSupport {
    private static final String SCHEMA = "customer_migration_test";
    private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";

    @Autowired
    private PostgreSQLContainer<?> postgres;

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void createSchema() {
        dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA " + SCHEMA);
    }

    @AfterEach
    void dropSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @Test
    void assignsExistingDataToDemo() throws Exception {
        migrate(false, true);
        jdbc.update("INSERT INTO " + SCHEMA + ".items (title, price) VALUES ('Legacy item', 100.00)");
        jdbc.update("INSERT INTO " + SCHEMA + ".cart_items (item_id, quantity) VALUES (1, 2)");
        jdbc.update("INSERT INTO " + SCHEMA + ".orders DEFAULT VALUES");
        jdbc.update("INSERT INTO " + SCHEMA + ".order_items (order_id, item_id, title, price, quantity)"
                + " VALUES (1, 1, 'Legacy item', 100.00, 2)");

        migrate(false, false);

        assertThat(cartOwner()).isEqualTo("demo");
        assertThat(orderOwners()).containsExactly("demo");
        assertThat(jdbc.queryForObject("SELECT quantity FROM " + SCHEMA + ".cart_items", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT quantity FROM " + SCHEMA + ".order_items", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name IN ('cart_items', 'orders')
                  AND column_name = 'user_id' AND column_default IS NULL AND is_nullable = 'NO'
                """, String.class, SCHEMA)).containsExactlyInAnyOrder("cart_items", "orders");
    }

    @Test
    void assignsSeedOrdersToDemoOnFreshInstallAndReservesItsIdentity() throws Exception {
        migrate(true, false);

        assertThat(orderOwners()).containsOnly("demo").hasSize(3);
        Long userId = jdbc.queryForObject("INSERT INTO " + SCHEMA
                + ".users (username, password_hash) VALUES ('customer', 'hash') RETURNING id", Long.class);
        Long demoId = jdbc.queryForObject("SELECT id FROM " + SCHEMA + ".users WHERE username = 'demo'", Long.class);
        assertThat(demoId).isEqualTo(1L);
        assertThat(userId).isGreaterThan(demoId);
    }

    private String cartOwner() {
        return jdbc.queryForObject("SELECT u.username FROM " + SCHEMA + ".cart_items c JOIN " + SCHEMA
                + ".users u ON u.id = c.user_id", String.class);
    }

    private List<String> orderOwners() {
        return jdbc.queryForList("SELECT u.username FROM " + SCHEMA + ".orders o JOIN " + SCHEMA
                + ".users u ON u.id = o.user_id", String.class);
    }

    private void migrate(boolean demoData, boolean sprintSevenOnly) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(SCHEMA);
            database.setLiquibaseSchemaName(SCHEMA);
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.setChangeLogParameter("appSchema", SCHEMA);
                liquibase.setChangeLogParameter("insertDemoData", Boolean.toString(demoData));
                if (sprintSevenOnly) {
                    liquibase.update(2, new Contexts(), new LabelExpression());
                } else {
                    liquibase.update(new Contexts(), new LabelExpression());
                }
            }
        }
    }
}
