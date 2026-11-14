package ru.practicum.yakovlev.mymarketapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.security.CustomerPrincipal;

import static ru.practicum.yakovlev.mymarketapp.support.CustomerWebSession.DEMO_PASSWORD_HASH;
import static ru.practicum.yakovlev.mymarketapp.support.CustomerWebSession.DEMO_USERNAME;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.TEST_USER_ID;

@TestConfiguration
public class CustomerDetailsConfiguration {
    @Bean(autowireCandidate = false)
    public ReactiveUserDetailsService testCustomerDetailsService() {
        return username -> Mono.just(username)
                .filter(DEMO_USERNAME::equals)
                .map(name -> new CustomerPrincipal(TEST_USER_ID, name, DEMO_PASSWORD_HASH, true));
    }
}
