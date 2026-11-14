package ru.practicum.yakovlev.payment.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.practicum.yakovlev.payment.config.PaymentJwtTestConfiguration;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;

import static ru.practicum.yakovlev.payment.config.PaymentJwtTestConfiguration.generateKeyPair;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://keycloak.test/realms/market")
@AutoConfigureWebTestClient
@Import(PaymentJwtTestConfiguration.class)
class PaymentSecurityTest {
    private static final String ISSUER = "https://keycloak.test/realms/market";
    private static final String AUDIENCE = "payments";
    private static final String CLIENT = "market-app";

    @Autowired
    private WebTestClient client;
    @Autowired
    private KeyPair signingKey;

    @Test
    void requiresBearerForBothOperations() {
        client.get().uri("/api/balance?userId=1").exchange().expectStatus().isUnauthorized();
        client.post().uri("/api/payments").bodyValue("{\"userId\":1,\"amount\":1}")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void marketClientCanDebitItsRequestedAccount() throws JOSEException {
        String token = token(ISSUER, AUDIENCE, CLIENT, Instant.now().plusSeconds(300), signingKey);
        client.post().uri("/api/payments").headers(headers -> headers.setBearerAuth(token))
                .header("Content-Type", "application/json").bodyValue("{\"userId\":17,\"amount\":12.50}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.balance").isEqualTo(99987.50);
        client.get().uri("/api/balance?userId=18").headers(headers -> headers.setBearerAuth(token))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.balance").isEqualTo(100000);
    }

    @Test
    void rejectsWrongClientAudienceIssuerExpiryAndSignature() throws JOSEException {
        Instant validUntil = Instant.now().plusSeconds(300);
        assertRejected(token(ISSUER, AUDIENCE, "different-client", validUntil, signingKey));
        assertRejected(token(ISSUER, "other-api", CLIENT, validUntil, signingKey));
        assertRejected(token("https://different-issuer.test", AUDIENCE, CLIENT, validUntil, signingKey));
        assertRejected(token(ISSUER, AUDIENCE, CLIENT, Instant.now().minusSeconds(300), signingKey));
        assertRejected(token(ISSUER, AUDIENCE, CLIENT, validUntil, generateKeyPair()));
    }

    private void assertRejected(String token) {
        client.get().uri("/api/balance?userId=1").headers(headers -> headers.setBearerAuth(token))
                .exchange().expectStatus().isUnauthorized();
    }

    private static String token(String issuer, String audience, String clientId, Instant expiresAt, KeyPair signer)
            throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("service-account-" + clientId)
                .audience(audience)
                .claim("azp", clientId)
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) signer.getPrivate()));
        return jwt.serialize();
    }

}
