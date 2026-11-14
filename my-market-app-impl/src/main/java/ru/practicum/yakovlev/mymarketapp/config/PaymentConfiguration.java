package ru.practicum.yakovlev.mymarketapp.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import ru.practicum.yakovlev.mymarketapp.payment.generated.ApiClient;
import ru.practicum.yakovlev.mymarketapp.payment.generated.api.PaymentsApi;

import java.time.Duration;

@Configuration
public class PaymentConfiguration {
    private static final String REGISTRATION_ID = "payments";

    @Bean
    public ReactiveOAuth2AuthorizedClientManager paymentAuthorizedClientManager(
            ReactiveClientRegistrationRepository registrations,
            ReactiveOAuth2AuthorizedClientService authorizedClients
    ) {
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(registrations, authorizedClients);
        manager.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }

    @Bean
    public PaymentsApi paymentsApi(
            WebClient.Builder builder,
            ReactiveOAuth2AuthorizedClientManager paymentAuthorizedClientManager,
            @Value("${spring.security.oauth2.client.registration.payments.client-id}") String clientId,
            @Value("${application.payment.base-url:http://localhost:8081}") String baseUrl,
            @Value("${application.payment.timeout-ms}") int timeoutMs
    ) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Payment timeout must be positive");
        }
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs));

        OAuth2AuthorizeRequest authorization = OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                .principal(clientId)
                .build();

        WebClient webClient = builder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter((request, next) -> paymentAuthorizedClientManager.authorize(authorization)
                        .switchIfEmpty(Mono.error(new IllegalStateException("Payment client authorization is unavailable")))
                        .flatMap(client -> next.exchange(ClientRequest.from(request)
                                .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                                .build())))
                .build();

        ApiClient client = new ApiClient(webClient)
                .setBasePath(baseUrl);
        return new PaymentsApi(client);
    }
}
