package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.yakovlev.mymarketapp.service.ImageService;
import ru.practicum.yakovlev.mymarketapp.support.MvcTestSupport;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerWebMvcTest extends MvcTestSupport {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ImageService imageService;

    @Test
    void nestedPathIsPassedToServiceAndImageMediaTypeIsReturned() throws Exception {
        byte[] bytes = {1, 2, 3};
        when(imageService.getImage("coffee.jpg")).thenReturn(new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "coffee.jpg";
            }
        });
        mvc.perform(get("/images/coffee.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(bytes));

        verify(imageService).getImage("coffee.jpg");
    }

}
