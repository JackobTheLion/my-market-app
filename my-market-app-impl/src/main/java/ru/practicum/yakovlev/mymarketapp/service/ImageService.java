package ru.practicum.yakovlev.mymarketapp.service;

import org.springframework.core.io.Resource;

public interface ImageService {

    Resource getImage(String filename);
}
