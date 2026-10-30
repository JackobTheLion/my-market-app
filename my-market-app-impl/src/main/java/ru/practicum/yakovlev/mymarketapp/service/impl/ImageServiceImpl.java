package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.practicum.yakovlev.mymarketapp.service.ImageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@Service
public class ImageServiceImpl implements ImageService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private final Path root;
    private final Resource defaultImage;

    public ImageServiceImpl(@Value("${application.images.root}") String root,
                            @Value("${application.images.default-image}") String defaultImage) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.defaultImage = new PathResource(this.root.resolve(defaultImage));
    }

    @Override
    public Mono<Resource> getImage(String filename) {
        return Mono.fromCallable(() -> resolveImage(filename))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> saveImage(FilePart image) {
        return Mono.defer(() -> {
            MediaType contentType = image.headers().getContentType();
            String format;
            if (MediaType.IMAGE_PNG.equals(contentType)) {
                format = "png";
            } else if (MediaType.IMAGE_JPEG.equals(contentType)) {
                format = "JPEG";
            } else {
                return Mono.error(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Only PNG and JPEG images are supported"));
            }
            return Mono.usingWhen(
                    Mono.fromCallable(() -> createUpload(format)).subscribeOn(Schedulers.boundedElastic()),
                    path -> image.transferTo(path)
                            .then(Mono.fromCallable(() -> {
                                validateImage(path, format);
                                return "uploads/" + path.getFileName();
                            }).subscribeOn(Schedulers.boundedElastic())),
                    path -> Mono.empty(),
                    (path, error) -> deleteFile(path),
                    this::deleteFile);
        });
    }

    @Override
    public Mono<Void> deleteImage(String filename) {
        return Mono.fromCallable(() -> {
                    Path path = root.resolve(filename).normalize();
                    Path realRoot = root.toRealPath();
                    if (!path.startsWith(root.resolve("uploads"))
                            || !path.getParent().toRealPath().startsWith(realRoot)) {
                        throw new IllegalArgumentException("Image is outside uploads directory");
                    }
                    return Files.deleteIfExists(path);
                })
                .subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Path createUpload(String format) throws IOException {
        Files.createDirectories(root);
        Path realRoot = root.toRealPath();
        Path uploads = Files.createDirectories(realRoot.resolve("uploads")).toRealPath();
        if (!uploads.startsWith(realRoot)) {
            throw new IOException("Uploads directory is outside images root");
        }
        return Files.createTempFile(uploads, "upload-", "." + format.toLowerCase(java.util.Locale.ROOT));
    }

    private void validateImage(Path path, String format) throws IOException {
        long size = Files.size(path);
        if (size > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image exceeds 5 MiB");
        }
        if (size > 0) {
            try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(input);
                        if (format.equalsIgnoreCase(reader.getFormatName())
                                && reader.getWidth(0) > 0 && reader.getHeight(0) > 0) {
                            return;
                        }
                    } finally {
                        reader.dispose();
                    }
                }
            } catch (IOException error) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image", error);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or empty image");
    }

    private Mono<Void> deleteFile(Path path) {
        return Mono.fromCallable(() -> Files.deleteIfExists(path))
                .subscribeOn(Schedulers.boundedElastic()).then();
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
