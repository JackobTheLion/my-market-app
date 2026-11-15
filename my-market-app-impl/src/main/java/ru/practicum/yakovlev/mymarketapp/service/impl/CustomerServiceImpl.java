package ru.practicum.yakovlev.mymarketapp.service.impl;

import io.r2dbc.postgresql.api.PostgresqlException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.practicum.yakovlev.mymarketapp.model.Customer;
import ru.practicum.yakovlev.mymarketapp.repository.CustomerRepository;
import ru.practicum.yakovlev.mymarketapp.service.CustomerService;
import ru.practicum.yakovlev.mymarketapp.service.RegistrationResult;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,64}");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<RegistrationResult> register(String username, String password) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return Mono.just(RegistrationResult.INVALID_USERNAME);
        }
        if (!isValidPassword(password)) {
            return Mono.just(RegistrationResult.INVALID_PASSWORD);
        }
        return Mono.fromCallable(() -> passwordEncoder.encode(password))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(hash -> customerRepository.save(new Customer(username, hash)))
                .thenReturn(RegistrationResult.REGISTERED)
                .onErrorReturn(CustomerServiceImpl::isUsernameConflict, RegistrationResult.USERNAME_TAKEN);
    }

    private static boolean isValidPassword(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        int length = password.codePointCount(0, password.length());
        return length >= MIN_PASSWORD_LENGTH && length <= MAX_PASSWORD_LENGTH;
    }

    private static boolean isUsernameConflict(Throwable error) {
        return error instanceof DuplicateKeyException
                && error.getCause() instanceof PostgresqlException postgresError
                && postgresError.getErrorDetails().getConstraintName().filter("users_username_unique"::equals).isPresent();
    }
}
