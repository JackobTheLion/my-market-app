package ru.practicum.yakovlev.mymarketapp.api.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(ApiConstants.IMAGES_PATH)
public interface ImageControllerApi {

    @GetMapping("/{*filename}")
    ResponseEntity<Resource> getImage(@PathVariable("filename") @NotBlank String filename);

}
