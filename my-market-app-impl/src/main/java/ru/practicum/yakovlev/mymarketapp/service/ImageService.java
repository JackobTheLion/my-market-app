package ru.practicum.yakovlev.mymarketapp.service;

import reactor.core.publisher.Mono;
import org.springframework.core.io.Resource;

public interface ImageService {

    Mono<Resource> getImage(String filename);
}
