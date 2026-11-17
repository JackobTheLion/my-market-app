package ru.practicum.yakovlev.mymarketapp.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.config.PaymentConfiguration;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException.Reason;
import ru.practicum.yakovlev.mymarketapp.payment.impl.HttpPaymentGateway;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentConfigurationTest {
    private static final int TIMEOUT_MS = 3000;
    private static final String CLIENT_ID = "market-app";

    private ReactiveOAuth2AuthorizedClientManager manager;

    @BeforeEach
    void setUp() {
        manager = mock(ReactiveOAuth2AuthorizedClientManager.class);
    }

    @Test
    void sendsServiceTokenAndAccountIdToPaymentApi() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("payments")
                .clientId(CLIENT_ID)
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("https://keycloak.test/token")
                .build();
        Instant now = Instant.now();
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "service-token", now, now.plusSeconds(300));
        when(manager.authorize(any())).thenReturn(Mono.just(new OAuth2AuthorizedClient(registration, CLIENT_ID, token)));
        ExchangeFunction exchange = request -> {
            assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer service-token");
            assertThat(request.url().getPath()).isEqualTo("/api/balance");
            assertThat(request.url().getQuery()).isEqualTo("userId=17");
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"balance\":125.50}").build());
        };

        StepVerifier.create(gateway(exchange).getBalance(17))
                .expectNext(new BigDecimal("125.50"))
                .verifyComplete();

        ArgumentCaptor<OAuth2AuthorizeRequest> authorization = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(manager).authorize(authorization.capture());
        assertThat(authorization.getValue().getClientRegistrationId()).isEqualTo("payments");
        assertThat(authorization.getValue().getPrincipal().getName()).isEqualTo(CLIENT_ID);
    }

    @Test
    void doesNotCallPaymentApiWhenClientAuthorizationFails() {
        when(manager.authorize(any())).thenReturn(Mono.empty());
        ExchangeFunction exchange = mock(ExchangeFunction.class);

        StepVerifier.create(gateway(exchange).pay(17, BigDecimal.ONE))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOfSatisfying(PaymentException.class,
                        payment -> assertThat(payment.getReason()).isEqualTo(Reason.UNAVAILABLE)))
                .verify();

        verifyNoInteractions(exchange);
    }

    private HttpPaymentGateway gateway(ExchangeFunction exchange) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        return new HttpPaymentGateway(new PaymentConfiguration()
                .paymentsApi(builder, manager, CLIENT_ID, "https://payments.test", TIMEOUT_MS), TIMEOUT_MS);
    }
}
