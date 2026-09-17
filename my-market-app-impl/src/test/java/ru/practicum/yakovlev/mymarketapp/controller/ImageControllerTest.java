package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.yakovlev.mymarketapp.service.impl.ImageServiceImpl;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

    @TempDir
    private Path directory;

    private MockMvc mvc;

    private final byte[] imageBytes = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9};

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectory(directory.resolve("demo"));
        Files.write(directory.resolve("demo/coffee.jpg"), imageBytes);
        Files.writeString(directory.resolve("placeholder.svg"), "<svg xmlns=\"http://www.w3.org/2000/svg\"/>");
        mvc = MockMvcBuilders.standaloneSetup(
                new ImageController(new ImageServiceImpl(directory.toString(), "placeholder.svg"))).build();
    }

    @Test
    void servesNestedImageFromFilesystem() throws Exception {
        mvc.perform(get("/images/demo/coffee.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    void servesPlaceholderForMissingImage() throws Exception {
        byte[] expected = Files.readAllBytes(directory.resolve("placeholder.svg"));
        mvc.perform(get("/images/demo/missing.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(content().bytes(expected));
    }
}
