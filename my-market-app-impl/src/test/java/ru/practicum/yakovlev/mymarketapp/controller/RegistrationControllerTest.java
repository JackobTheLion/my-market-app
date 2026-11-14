package ru.practicum.yakovlev.mymarketapp.controller;

import io.r2dbc.postgresql.api.ErrorDetails;
import io.r2dbc.postgresql.api.PostgresqlException;
import io.r2dbc.postgresql.message.backend.Field;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.model.Customer;
import ru.practicum.yakovlev.mymarketapp.support.CustomerWebSession;
import ru.practicum.yakovlev.mymarketapp.support.WebFluxTestSupport;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.CustomerWebSession.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.TEST_USER_ID;

class RegistrationControllerTest extends WebFluxTestSupport {

    @Test
    void showsAnErrorForADuplicateLogin() {
        DuplicateKeyException error = duplicateKey("users_username_unique");
        when(customers.save(any(Customer.class))).thenReturn(Mono.error(error));

        submitRegistration("long-password").expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("Этот логин уже занят").doesNotContain("long-password"));
    }

    @Test
    void otherDuplicateKeysDoNotLookLikeATakenLogin() {
        DuplicateKeyException error = duplicateKey("users_pkey");
        when(customers.save(any(Customer.class))).thenReturn(Mono.error(error));

        submitRegistration("long-password").expectStatus().is5xxServerError();
    }

    @Test
    void guestCannotAccessPrivateRoutes() {
        webTestClient.get().uri("/cart/items").exchange().expectStatus().is3xxRedirection().expectHeader().location("/login");
        webTestClient.get().uri("/orders").exchange().expectStatus().is3xxRedirection().expectHeader().location("/login");
        webTestClient.get().uri("/items/new").exchange().expectStatus().is3xxRedirection().expectHeader().location("/login");
        webTestClient.post().uri("/buy").exchange().expectStatus().isForbidden();
    }

    @Test
    void logoutExpiresTheSessionCookieAndRevokesThePreviousSession() {
        when(customers.findByUsername(DEMO_USERNAME)).thenReturn(
                Mono.just(new Customer(TEST_USER_ID, DEMO_USERNAME, DEMO_PASSWORD_HASH, true)));
        EntityExchangeResult<Void> login = client.post().uri("/login")
                .body(BodyInserters.fromFormData("username", DEMO_USERNAME).with("password", DEMO_PASSWORD))
                .exchange().expectStatus().is3xxRedirection().expectHeader().location("/items")
                .expectBody().isEmpty();
        String session = sessionId(login);

        webTestClient.get().uri("/items/new").cookie("SESSION", session).exchange().expectStatus().isOk();

        client.post().uri("/logout").cookie("SESSION", session).exchange().expectStatus().is3xxRedirection()
                .expectCookie().maxAge("SESSION", Duration.ZERO);
        webTestClient.get().uri("/items/new").cookie("SESSION", session).exchange().expectStatus().is3xxRedirection()
                .expectHeader().location("/login");
    }

    private WebTestClient.ResponseSpec submitRegistration(String password) {
        EntityExchangeResult<String> form = webTestClient.get().uri("/register").exchange()
                .expectStatus().isOk().expectBody(String.class).returnResult();
        return webTestClient.post().uri("/register")
                .cookie("SESSION", CustomerWebSession.sessionId(form))
                .body(BodyInserters.fromFormData("username", "anna").with("password", password)
                        .with("_csrf", CustomerWebSession.csrfToken(form.getResponseBody())))
                .exchange();
    }

    private static DuplicateKeyException duplicateKey(String constraint) {
        R2dbcDataIntegrityViolationException driverError = mock(R2dbcDataIntegrityViolationException.class,
                withSettings().extraInterfaces(PostgresqlException.class));
        when(((PostgresqlException) driverError).getErrorDetails()).thenReturn(new ErrorDetails(List.of(
                new Field(Field.FieldType.CODE, "23505"),
                new Field(Field.FieldType.MESSAGE, "Duplicate key"),
                new Field(Field.FieldType.CONSTRAINT_NAME, constraint))));
        return new DuplicateKeyException("Duplicate key", driverError);
    }
}
