package ru.practicum.yakovlev.mymarketapp.payment.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException.Reason;
import ru.practicum.yakovlev.mymarketapp.payment.PaymentGateway;
import ru.practicum.yakovlev.mymarketapp.payment.generated.api.PaymentsApi;
import ru.practicum.yakovlev.mymarketapp.payment.generated.model.BalanceResponse;
import ru.practicum.yakovlev.mymarketapp.payment.generated.model.PaymentError;
import ru.practicum.yakovlev.mymarketapp.payment.generated.model.PaymentRequest;

import java.math.BigDecimal;
import java.time.Duration;

@Service
public class HttpPaymentGateway implements PaymentGateway {
    private final PaymentsApi api;
    private final Duration timeout;

    public HttpPaymentGateway(PaymentsApi api, @Value("${application.payment.timeout-ms}") int timeoutMs) {
        this.api = api;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Override
    public Mono<BigDecimal> getBalance(long userId) {
        return api.getBalance(userId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty balance response")))
                .map(this::balance)
                .timeout(timeout)
                .onErrorMap(this::translate);
    }

    @Override
    public Mono<Void> pay(long userId, BigDecimal amount) {
        //для простоты намеренно не вводим ключ идемпотентности платежа
        return api.makePayment(new PaymentRequest().userId(userId).amount(amount))
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty payment response")))
                .map(this::balance)
                .timeout(timeout)
                .onErrorMap(this::translate)
                .then();
    }

    private BigDecimal balance(BalanceResponse response) {
        if (response.getBalance().signum() < 0) {
            throw new IllegalStateException("Invalid balance response");
        }
        return response.getBalance();
    }

    private Throwable translate(Throwable error) {
        if (error instanceof WebClientResponseException response && response.getStatusCode().value() == 409) {
            PaymentError body;
            try {
                body = response.getResponseBodyAs(PaymentError.class);
            } catch (RuntimeException decodingError) {
                return new PaymentException(Reason.REJECTED, error);
            }
            if (body != null && "INSUFFICIENT_FUNDS".equals(body.getCode())) {
                return new PaymentException(Reason.INSUFFICIENT_FUNDS, error);
            }
            return new PaymentException(Reason.REJECTED, error);
        }
        return new PaymentException(Reason.UNAVAILABLE, error);
    }
}
