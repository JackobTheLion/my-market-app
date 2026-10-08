package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.apache.el.lang.ELArithmetic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.service.ImageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Service
public class ImageServiceImpl implements ImageService {

    private final Path root;
    private final Resource defaultImage;

    public ImageServiceImpl(@Value("${application.images.root}") String root,
                            @Value("${application.images.default-image}") String defaultImage) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.defaultImage = new PathResource(this.root.resolve(defaultImage));
    }

    @Override
    public Mono<Resource> getImage(String filename) {
        return Mono.just(resolveImage(filename));
    }

    private Resource resolveImage(String filename) {
        if (filename == null || filename.isBlank() || filename.indexOf('\\') >= 0) {
            return defaultImage;
        }
        try {
            Path relative = Path.of(filename).normalize();
            if (relative.isAbsolute() || relative.startsWith("..")) {
                return defaultImage;
            }
            Path candidate = root.resolve(relative).normalize();
            if (!candidate.startsWith(root)) {
                return defaultImage;
            }
            Path realRoot = root.toRealPath();
            Path realFile = candidate.toRealPath();
            if (!realFile.startsWith(realRoot) || !Files.isRegularFile(realFile) || !Files.isReadable(realFile)) {
                return defaultImage;
            }
            return new PathResource(realFile);
        } catch (IOException | InvalidPathException e) {
            return defaultImage;
        }
    }
}
