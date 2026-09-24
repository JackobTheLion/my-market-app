package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageServiceImplTest {

    @TempDir
    private Path directory;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "missing.jpg", "invalid\0path.jpg", ".", "../outside.jpg", "..\\outside.jpg"})
    void invalidMissingOrDirectoryPathReturnsPlaceholder(String filename) throws Exception {
        Files.writeString(directory.resolve("default-image.svg"), "placeholder");
        ImageServiceImpl service = new ImageServiceImpl(directory.toString(), "default-image.svg");
        assertEquals("placeholder", service.getImage(filename).getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void readsNestedFileFromConfiguredDirectory() throws Exception {
        Path demo = Files.createDirectory(directory.resolve("demo"));
        byte[] expected = {1, 2, 3};
        Files.write(demo.resolve("photo.jpg"), expected);
        ImageServiceImpl service = new ImageServiceImpl(directory.toString(), "default-image.svg");
        assertArrayEquals(expected, service.getImage("demo/photo.jpg").getContentAsByteArray());
    }

    @Test
    void rejectsPathsOutsideFilesystemRoot() throws Exception {
        byte[] placeholder = "placeholder".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(directory.resolve("default-image.svg"), placeholder);
        ImageServiceImpl service = new ImageServiceImpl(directory.toString(), "default-image.svg");
        assertArrayEquals(placeholder, service.getImage("../application.yaml").getContentAsByteArray());
        assertArrayEquals(placeholder, service.getImage("..\\application.yaml").getContentAsByteArray());
        assertArrayEquals(placeholder, service.getImage("/application.yaml").getContentAsByteArray());
    }

    @Test
    void rejectsSymlinkOutsideFilesystemRoot() throws Exception {
        Path root = Files.createDirectory(directory.resolve("images"));
        Path outside = Files.writeString(directory.resolve("outside.jpg"), "private");
        Files.writeString(root.resolve("default-image.svg"), "placeholder");
        Files.createSymbolicLink(root.resolve("photo.jpg"), outside);
        ImageServiceImpl service = new ImageServiceImpl(root.toString(), "default-image.svg");
        assertEquals("default-image.svg", service.getImage("photo.jpg").getFilename());
        assertEquals("placeholder", service.getImage("photo.jpg").getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
}
