package ru.practicum.yakovlev.mymarketapp.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException;
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

    public HttpPaymentGateway(PaymentsApi api, @Value("${application.payment.timeout:PT3S}") Duration timeout) {
        this.api = api;
        this.timeout = timeout;
    }

    @Override
    public Mono<BigDecimal> getBalance() {
        return api.getBalance().switchIfEmpty(Mono.error(new IllegalStateException("Empty balance response")))
                .map(this::balance).timeout(timeout).onErrorMap(this::translate);
    }

    @Override
    public Mono<Void> pay(BigDecimal amount) {
        //для простоты намеренно не вводим ключ идемпотентности платежа
        return api.makePayment(new PaymentRequest().amount(amount))
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
                return new PaymentException("rejected", "Платёж отклонён", error);
            }
            if (body != null && "INSUFFICIENT_FUNDS".equals(body.getCode())) {
                return new PaymentException("insufficient-funds", "Недостаточно средств для оплаты заказа", error);
            }
            return new PaymentException("rejected", "Платёж отклонён", error);
        }
        return new PaymentException("unavailable", "Сервис платежей недоступен. Попробуйте позже", error);
    }
}
