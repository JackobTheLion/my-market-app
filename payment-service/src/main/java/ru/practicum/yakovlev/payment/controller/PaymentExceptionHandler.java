package ru.practicum.yakovlev.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import ru.practicum.yakovlev.payment.generated.model.PaymentError;
import ru.practicum.yakovlev.payment.service.PaymentRejectedException;

@RestControllerAdvice
public class PaymentExceptionHandler {
    @ExceptionHandler(PaymentRejectedException.class)
    public ResponseEntity<PaymentError> rejected(PaymentRejectedException exception) {
        return ResponseEntity.status(409).body(new PaymentError().code(exception.getCode()).message(exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, ServerWebInputException.class})
    public ResponseEntity<PaymentError> invalid(Exception exception) {
        return ResponseEntity.badRequest().body(new PaymentError().code("INVALID_REQUEST").message("Invalid payment request"));
    }
}
