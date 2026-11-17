package ru.practicum.yakovlev.mymarketapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
import org.springframework.web.server.WebSession;
import ru.practicum.yakovlev.mymarketapp.repository.CustomerRepository;
import ru.practicum.yakovlev.mymarketapp.security.CustomerPrincipal;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveUserDetailsService customerDetailsService(CustomerRepository customers) {
        return username -> customers.findByUsername(username)
                .map(customer -> new CustomerPrincipal(customer.getId(), customer.getUsername(),
                        customer.getPasswordHash(), customer.isEnabled()));
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveUserDetailsService customers,
            PasswordEncoder passwordEncoder
    ) {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager =
                new UserDetailsRepositoryReactiveAuthenticationManager(customers);
        authenticationManager.setPasswordEncoder(passwordEncoder);

        XorServerCsrfTokenRequestAttributeHandler csrfHandler = new XorServerCsrfTokenRequestAttributeHandler();
        csrfHandler.setTokenFromMultipartDataEnabled(true);

        return http
                .authenticationManager(authentication -> authenticationManager.authenticate(authentication)
                        .doOnNext(authenticated -> {
                            if (authenticated instanceof CredentialsContainer credentials) {
                                credentials.eraseCredentials();
                            }
                        }))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/login", "/register").permitAll()
                        .pathMatchers("/items/new").authenticated()
                        .pathMatchers(HttpMethod.GET, "/", "/items", "/items/{id}", "/images/**").permitAll()
                        .anyExchange().authenticated())
                .csrf(csrf -> csrf.csrfTokenRequestHandler(csrfHandler))
                .formLogin(form -> form.authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/items")))
                .logout(logout -> logout.logoutHandler((exchange, authentication) ->
                        exchange.getExchange().getSession().flatMap(WebSession::invalidate)))
                .build();
    }
}
