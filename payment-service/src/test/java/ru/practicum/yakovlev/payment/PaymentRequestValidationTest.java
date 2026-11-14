package ru.practicum.yakovlev.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.payment.controller.PaymentController;
import ru.practicum.yakovlev.payment.config.PaymentSecurityConfiguration;
import ru.practicum.yakovlev.payment.service.AccountService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
@Import(PaymentSecurityConfiguration.class)
class PaymentRequestValidationTest {
    @Autowired
    private WebTestClient guest;
    private WebTestClient client;

    @MockitoBean
    private ReactiveJwtDecoder decoder;
    @MockitoBean
    private AccountService account;

    @BeforeEach
    void authorizeClient() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("service-account-market-app")
                .build();
        when(decoder.decode("test-token")).thenReturn(Mono.just(jwt));
        client = guest.mutate().defaultHeaders(headers -> headers.setBearerAuth("test-token")).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/balance", "/api/balance?userId=0", "/api/balance?userId=-1"})
    void balanceRequiresPositiveUserId(String uri) {
        client.get().uri(uri).exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");
        verifyNoInteractions(account);
    }

    @Test
    void balanceLoadsTheRequestedAccount() {
        when(account.getBalance(17L)).thenReturn(Mono.just(new BigDecimal("125.00")));
        client.get().uri("/api/balance?userId=17").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.balance").isEqualTo(125);
        verify(account).getBalance(17L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"amount\":1}", "{\"userId\":0,\"amount\":1}", "{\"userId\":-1,\"amount\":1}"})
    void paymentRequiresPositiveUserId(String body) {
        client.post().uri("/api/payments").contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");
        verifyNoInteractions(account);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "-1", "0.001", "1.001", "10.000",
            "99999999999999999.999", "100000000000000000.00", "1E+17"})
    void invalidAmountIsRejectedBeforeCallingService(String amount) {
        client.post().uri("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"userId\":1,\"amount\":" + amount + "}")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");

        verifyNoInteractions(account);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "[]", "malformed"})
    void invalidBodyIsRejectedBeforeCallingService(String body) {
        client.post().uri("/api/payments").contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");

        verifyNoInteractions(account);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "10.00", "99999999999999999.99", "1E+16"})
    void validAmountIncludingBoundsReachesService(String amount) {
        BigDecimal expected = new BigDecimal(amount);
        when(account.pay(eq(1L), argThat(value -> value.compareTo(expected) == 0)))
                .thenReturn(Mono.just(BigDecimal.ZERO));

        client.post().uri("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"userId\":1,\"amount\":" + amount + "}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.balance").isEqualTo(0);

        verify(account).pay(eq(1L), argThat(value -> value.compareTo(expected) == 0));
    }
}
