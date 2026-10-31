package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveMultipartAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.service.ItemService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebFluxTest(ItemCreateController.class)
@ImportAutoConfiguration(ReactiveMultipartAutoConfiguration.class)
class ItemCreateControllerTest {
    @Autowired
    private WebTestClient client;
    @MockitoBean
    private ItemService items;

    @Test
    void rendersFormWithFileUploadAndCatalogLink() {
        client.get().uri("/items/new").exchange().expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html)
                        .contains("Добавить товар", "multipart/form-data", "name=\"image\"", "href=\"/items\""));
        verifyNoInteractions(items);
    }

    @Test
    void formSubmitsMetadataAndFileAndRedirectsToCreatedProduct() {
        when(items.createItem(any())).thenReturn(Mono.just(42L));
        client.post().uri("/items/new").bodyValue(form("Coffee", "12.50", true).build())
                .exchange().expectStatus().isSeeOther().expectHeader().location("/items/42");
        verify(items).createItem(argThat(request -> request.getTitle().equals("Coffee")
                && request.getDescription().equals("Beans") && request.getPrice().toPlainString().equals("12.50")
                && request.getImage().filename().equals("coffee.png")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.001", "100000000000000000.00", "not-a-price", ""})
    void invalidPriceDisplaysFormAndPreservesText(String price) {
        client.post().uri("/items/new").bodyValue(form("Coffee", price, true).build())
                .exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("Coffee", "Beans", "Укажите положительную цену"));
        verifyNoInteractions(items);
    }

    @Test
    void missingFileDisplaysValidationError() {
        client.post().uri("/items/new").bodyValue(form("Coffee", "12.50", false).build())
                .exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("Выберите изображение товара", "Coffee"));
        verifyNoInteractions(items);
    }

    @Test
    void blankTitleDisplaysValidationError() {
        client.post().uri("/items/new").bodyValue(form("", "12.50", true).build())
                .exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("Укажите название товара"));
        verifyNoInteractions(items);
    }

    @Test
    void invalidImageDisplaysFriendlyErrorAndPreservesValues() {
        when(items.createItem(any())).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image")));
        client.post().uri("/items/new").bodyValue(form("Coffee", "12.50", true).build())
                .exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("Выберите корректное изображение PNG или JPEG", "Coffee", "Beans"));
    }

    @Test
    void databaseFailureDisplaysFormWithoutRedirect() {
        when(items.createItem(any())).thenReturn(Mono.error(new IllegalStateException("Database unavailable")));
        client.post().uri("/items/new").bodyValue(form("Coffee", "12.50", true).build())
                .exchange().expectStatus().isOk().expectHeader().doesNotExist("Location")
                .expectBody(String.class).value(html -> assertThat(html)
                        .contains("Не удалось добавить товар", "Coffee")
                        .doesNotContain("Database unavailable"));
    }

    @Test
    void oversizedUploadDisplaysErrorWithoutCallingService() {
        MultipartBodyBuilder form = form("Coffee", "12.50", false);
        form.part("image", new ByteArrayResource(new byte[5 * 1024 * 1024 + 1]))
                .filename("large.png").contentType(MediaType.IMAGE_PNG);
        client.post().uri("/items/new").bodyValue(form.build())
                .exchange().expectStatus().isEqualTo(413).expectBody(String.class)
                .value(html -> assertThat(html).contains("Размер изображения не должен превышать 5 МиБ"));
        verifyNoInteractions(items);
    }

    private MultipartBodyBuilder form(String title, String price, boolean includeImage) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("title", title);
        body.part("description", "Beans");
        body.part("price", price);
        if (includeImage) {
            body.part("image", new ByteArrayResource(new byte[]{1, 2, 3}))
                    .filename("coffee.png").contentType(MediaType.IMAGE_PNG);
        }
        return body;
    }
}
