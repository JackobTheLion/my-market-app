package ru.practicum.yakovlev.mymarketapp.payment;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import ru.practicum.yakovlev.mymarketapp.payment.generated.ApiClient;
import ru.practicum.yakovlev.mymarketapp.payment.generated.api.PaymentsApi;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class PaymentConfiguration {
    @Bean
    PaymentsApi paymentsApi(WebClient.Builder builder,
                            @Value("${application.payment.base-url:http://localhost:8081}") String baseUrl,
                            @Value("${application.payment.timeout:PT3S}") Duration timeout) {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Payment timeout must be positive");
        }
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(timeout.toMillis()))
                .responseTimeout(timeout);
        ApiClient client = new ApiClient(builder.clientConnector(new ReactorClientHttpConnector(httpClient)).build());
        client.setBasePath(baseUrl);
        return new PaymentsApi(client);
    }
}
