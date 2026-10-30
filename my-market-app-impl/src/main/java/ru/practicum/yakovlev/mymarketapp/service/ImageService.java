package ru.practicum.yakovlev.mymarketapp.service;

import reactor.core.publisher.Mono;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.FilePart;

public interface ImageService {

    Mono<Resource> getImage(String filename);

    Mono<String> saveImage(FilePart image);

    Mono<Void> deleteImage(String filename);
}
