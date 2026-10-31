package ru.practicum.yakovlev.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.payment.generated.api.PaymentsApi;
import ru.practicum.yakovlev.payment.generated.model.BalanceResponse;
import ru.practicum.yakovlev.payment.generated.model.PaymentRequest;
import ru.practicum.yakovlev.payment.service.AccountService;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentsApi {
    private final AccountService account;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange ignore) {
        return account.getBalance()
                .map(balance -> ResponseEntity.ok(new BalanceResponse().balance(balance)));
    }

    @Override
    public Mono<ResponseEntity<BalanceResponse>> makePayment(Mono<PaymentRequest> paymentRequest, ServerWebExchange ignore) {
        return paymentRequest.flatMap(request -> account.pay(request.getAmount()))
                .map(balance -> ResponseEntity.ok(new BalanceResponse().balance(balance)));
    }
}
