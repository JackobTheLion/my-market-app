package ru.practicum.yakovlev.mymarketapp.support;

import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public final class CustomerWebSession {
    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "demo-market";
    public static final String DEMO_PASSWORD_HASH = "$2a$10$5FGEzIuZzMXsEnp/5y2E.e7I3QMJxYHv9ItOJLuZX5i.ppFu1jmjO";

    private static final Pattern CSRF_INPUT = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*>");
    private static final Pattern INPUT_VALUE = Pattern.compile("value=\"([^\"]+)\"");

    private CustomerWebSession() {
    }

    public static void register(WebTestClient guest, String username, String password) {
        EntityExchangeResult<String> page = guest.get().uri("/register").exchange()
                .expectStatus().isOk().expectBody(String.class).returnResult();
        guest.post().uri("/register").cookie("SESSION", sessionId(page))
                .body(BodyInserters.fromFormData("username", username)
                        .with("password", password)
                        .with("_csrf", csrfToken(page.getResponseBody())))
                .exchange().expectStatus().isSeeOther().expectHeader().location("/login");
    }

    public static WebTestClient login(WebTestClient guest, String username, String password) {
        EntityExchangeResult<String> page = guest.get().uri("/login").exchange()
                .expectStatus().isOk().expectBody(String.class).returnResult();
        EntityExchangeResult<Void> login = guest.post().uri("/login")
                .cookie("SESSION", sessionId(page))
                .body(BodyInserters.fromFormData("username", username)
                        .with("password", password)
                        .with("_csrf", csrfToken(page.getResponseBody())))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items")
                .expectBody().isEmpty();
        String sessionId = sessionId(login);
        String logoutPage = guest.get().uri("/logout").cookie("SESSION", sessionId)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        return guest.mutate().defaultCookie("SESSION", sessionId)
                .defaultHeader("X-CSRF-TOKEN", csrfToken(logoutPage))
                .build();
    }

    public static String sessionId(EntityExchangeResult<?> response) {
        ResponseCookie cookie = response.getResponseCookies().getFirst("SESSION");
        assertThat(cookie).isNotNull();
        return Objects.requireNonNull(cookie).getValue();
    }

    public static String csrfToken(String html) {
        Matcher input = CSRF_INPUT.matcher(Objects.requireNonNull(html));
        assertThat(input.find()).as("CSRF input").isTrue();
        Matcher value = INPUT_VALUE.matcher(input.group());
        assertThat(value.find()).as("CSRF input value").isTrue();
        return value.group(1);
    }
}
