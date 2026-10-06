package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.support.WebFluxTestSupport;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

class ImageControllerTest extends WebFluxTestSupport {
    @Test
    void streamsNestedImageWithContentType() {
        byte[] bytes = {1, 2, 3};
        ByteArrayResource image = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "coffee.jpg";
            }
        };
        when(imageService.getImage("demo/coffee.jpg")).thenReturn(Mono.just(image));
        client.get()
                .uri("/images/demo/coffee.jpg")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.IMAGE_JPEG)
                .expectBody()
                .consumeWith(response ->
                        assertThat(response.getResponseBody()).isEqualTo(bytes));
    }

    @Test
    void fallbackUsesActualResourceContentType() {
        ByteArrayResource image = new ByteArrayResource("<svg/>".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "default-image.svg";
            }
        };
        when(imageService.getImage("missing.jpg")).thenReturn(Mono.just(image));
        client.get()
                .uri("/images/missing.jpg")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.valueOf("image/svg+xml"));
    }
}
