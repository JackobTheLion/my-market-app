package ru.practicum.yakovlev.payment.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

@TestConfiguration
public class PaymentJwtTestConfiguration {
    @Bean
    KeyPair signingKey() {
        return generateKeyPair();
    }

    @Bean
    @Primary
    ReactiveJwtDecoder testJwtDecoder(KeyPair signingKey, OAuth2TokenValidator<Jwt> validator) {
        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder.withPublicKey((RSAPublicKey) signingKey.getPublic()).build();
        decoder.setJwtValidator(validator);
        return decoder;
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("RSA key generation failed", error);
        }
    }
}
