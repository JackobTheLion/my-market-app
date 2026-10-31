package ru.practicum.yakovlev.mymarketapp.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.payment.PaymentGateway;

import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
public abstract class IntegrationTestSupport extends DatabaseTestSupport {
    @MockitoBean
    protected PaymentGateway paymentGateway;
    @Autowired
    protected ReactiveStringRedisTemplate redis;

    @BeforeEach
    void resetExternalServices() {
        redis.execute(connection -> connection.serverCommands().flushDb()).then().block();
        lenient().when(paymentGateway.getBalance()).thenReturn(Mono.just(new BigDecimal("100000.00")));
        lenient().when(paymentGateway.pay(any())).thenReturn(Mono.empty());
    }
}
