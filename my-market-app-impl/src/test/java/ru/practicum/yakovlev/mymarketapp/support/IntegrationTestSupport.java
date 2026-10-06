package ru.practicum.yakovlev.mymarketapp.support;

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
public abstract class IntegrationTestSupport extends DatabaseTestSupport {
}
