package ru.practicum.yakovlev.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import java.util.Collection;

@Configuration
@EnableWebFluxSecurity
public class PaymentSecurityConfiguration {

    @Bean
    public SecurityWebFilterChain paymentSecurityFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder decoder) {
        return http
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.GET, "/api/balance").authenticated()
                        .pathMatchers(HttpMethod.POST, "/api/payments").authenticated()
                        .anyExchange().denyAll())
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtDecoder(decoder)))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder paymentJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            OAuth2TokenValidator<Jwt> paymentTokenValidator
    ) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(paymentTokenValidator);
        return decoder;
    }

    @Bean
    public OAuth2TokenValidator<Jwt> paymentTokenValidator(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${payment.oauth2.audience}") String audience,
            @Value("${payment.oauth2.client-id}") String clientId
    ) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<Collection<String>>("aud", audiences -> audiences != null && audiences.contains(audience)),
                new JwtClaimValidator<String>("azp", clientId::equals)
        );
    }
}
