package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveMultipartAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.mapper.ItemMapper;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ProductCacheRepository;
import ru.practicum.yakovlev.mymarketapp.service.impl.ImageServiceImpl;
import ru.practicum.yakovlev.mymarketapp.service.impl.ItemServiceImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.imageBytes;

@WebFluxTest({ItemCreateController.class, ImageController.class})
@Import({ItemServiceImpl.class, ImageServiceImpl.class})
@ImportAutoConfiguration(ReactiveMultipartAutoConfiguration.class)
class ItemCreateHttpTest {
    @TempDir private static Path directory;
    @Autowired private WebTestClient client;
    @MockitoBean private ItemRepository items;
    @MockitoBean private ProductCacheRepository cache;
    @MockitoBean private CartItemRepository cart;
    @MockitoBean private ItemMapper mapper;

    @DynamicPropertySource
    static void imageRoot(DynamicPropertyRegistry registry) {
        registry.add("application.images.root", () -> directory.toString());
    }

    @BeforeEach
    void clearUploads() throws Exception {
        Files.createDirectories(directory.resolve("uploads"));
        try (Stream<Path> files = Files.list(directory.resolve("uploads"))) {
            for (Path path : files.toList()) {
                Files.delete(path);
            }
        }
    }

    @Test
    void addsProductFromFormAndServesImage() throws Exception {
        when(items.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setId(42L);
            return Mono.just(item);
        });
        when(cache.invalidatePages()).thenReturn(Mono.empty());
        byte[] png = imageBytes("png");
        client.post().uri("/items/new").bodyValue(form(png).build())
                .exchange().expectStatus().isSeeOther().expectHeader().location("/items/42");
        String imagePath;
        try (Stream<Path> files = Files.list(directory.resolve("uploads"))) {
            imagePath = "uploads/" + files.findFirst().orElseThrow().getFileName();
        }
        client.get().uri("/images/" + imagePath).exchange().expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG).expectBody(byte[].class).isEqualTo(png);
        verify(items).save(argThat(item -> item.getTitle().equals("Coffee") && item.getImagePath().equals(imagePath)));
        verify(cache).invalidatePages();
    }

    @Test
    void invalidImageShowsErrorWithoutWritingProduct() throws Exception {
        client.post().uri("/items/new").bodyValue(form(new byte[]{1, 2, 3}).build())
                .exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("Выберите корректное изображение PNG или JPEG"));
        verifyNoInteractions(items, cache);
        assertUploadsEmpty();
    }

    @Test
    void failedInsertDeletesImageAndPreservesForm() throws Exception {
        when(items.save(any(Item.class))).thenReturn(Mono.error(new IllegalStateException("Insert failed")));
        client.post().uri("/items/new").bodyValue(form(imageBytes("png")).build())
                .exchange().expectStatus().isOk().expectBody(String.class)
                .value(html -> assertThat(html).contains("Не удалось добавить товар", "Coffee", "Beans"));
        verifyNoInteractions(cache);
        assertUploadsEmpty();
    }

    private MultipartBodyBuilder form(byte[] png) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("title", "Coffee");
        body.part("description", "Beans");
        body.part("price", "12.50");
        body.part("image", new ByteArrayResource(png)).filename("../../photo.png").contentType(MediaType.IMAGE_PNG);
        return body;
    }

    private void assertUploadsEmpty() throws Exception {
        try (Stream<Path> files = Files.list(directory.resolve("uploads"))) {
            assertThat(files).isEmpty();
        }
    }
}
