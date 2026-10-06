package ru.practicum.yakovlev.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.controller.ImageControllerApi;
import ru.practicum.yakovlev.mymarketapp.service.ImageService;

@Controller
@RequiredArgsConstructor
public class ImageController implements ImageControllerApi {

    private final ImageService imageService;

    @Override
    public Mono<ResponseEntity<Resource>> getImage(String filename) {
        String relativePath = filename.startsWith("/") ? filename.substring(1) : filename;
        return imageService.getImage(relativePath)
                .map(image -> {
                    MediaType mediaType = MediaTypeFactory.getMediaType(image)
                            .orElse(MediaType.APPLICATION_OCTET_STREAM);
                    return ResponseEntity.ok()
                            .contentType(mediaType)
                            .body(image);
                });
    }
}
