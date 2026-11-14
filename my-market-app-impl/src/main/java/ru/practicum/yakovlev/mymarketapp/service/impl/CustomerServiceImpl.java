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

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Boolean> register(String username, String password) {
        return Mono.fromCallable(() -> passwordEncoder.encode(password))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(hash -> customerRepository.save(new Customer(username, hash)))
                .thenReturn(true)
                .onErrorReturn(CustomerServiceImpl::isUsernameConflict, false);
    }

    private static boolean isUsernameConflict(Throwable error) {
        return error instanceof DuplicateKeyException
                && error.getCause() instanceof PostgresqlException postgresError
                && postgresError.getErrorDetails().getConstraintName().filter("users_username_unique"::equals).isPresent();
    }
}
