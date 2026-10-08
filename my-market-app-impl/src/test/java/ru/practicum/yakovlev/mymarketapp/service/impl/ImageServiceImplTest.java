package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ImageServiceImplTest {

    @TempDir
    private Path directory;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "missing.jpg", "invalid\0path.jpg", ".", "../outside.jpg", "..\\outside.jpg"})
    void invalidMissingOrDirectoryPathReturnsPlaceholder(String filename) throws Exception {
        Files.writeString(directory.resolve("default-image.svg"), "placeholder");
        ImageServiceImpl service = new ImageServiceImpl(directory.toString(), "default-image.svg");
        StepVerifier.create(service.getImage(filename))
                .assertNext(resource ->
                        assertArrayEquals("placeholder".getBytes(java.nio.charset.StandardCharsets.UTF_8), read(resource)))
                .verifyComplete();
    }

    @Test
    void readsNestedFileFromConfiguredDirectory() throws Exception {
        Path demo = Files.createDirectory(directory.resolve("demo"));
        byte[] expected = {1, 2, 3};
        Files.write(demo.resolve("photo.jpg"), expected);
        ImageServiceImpl service = new ImageServiceImpl(directory.toString(), "default-image.svg");
        StepVerifier.create(service.getImage("demo/photo.jpg"))
                .assertNext(resource ->
                        assertArrayEquals(expected, read(resource)))
                .verifyComplete();
    }

    @Test
    void rejectsPathsOutsideFilesystemRoot() throws Exception {
        byte[] placeholder = "placeholder".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(directory.resolve("default-image.svg"), placeholder);
        ImageServiceImpl service = new ImageServiceImpl(directory.toString(), "default-image.svg");
        StepVerifier.create(service.getImage("../application.yaml"))
                .assertNext(resource ->
                        assertArrayEquals(placeholder, read(resource)))
                .verifyComplete();
        StepVerifier.create(service.getImage("..\\application.yaml"))
                .assertNext(resource ->
                        assertArrayEquals(placeholder, read(resource)))
                .verifyComplete();
        StepVerifier.create(service.getImage("/application.yaml"))
                .assertNext(resource ->
                        assertArrayEquals(placeholder, read(resource)))
                .verifyComplete();
    }

    private static byte[] read(Resource resource) {
        try {
            return resource.getContentAsByteArray();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
