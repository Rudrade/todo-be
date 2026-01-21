package me.rudrade.todo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import jakarta.validation.constraints.NotNull;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.exception.UnexpectedErrorException;

@Service
public class S3Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Service.class);

    private static final long MAX_SIZE = 1572864L; // 1.5MB
    private static final String ALLOWED_EXTENSION = ".webp";

    @Value("${todo.app.user.image.bucket}")
    private String imageBucket;

    @Value("${todo.app.user.image.url}")
    private String imageUrl;

    public String uploadImage(@NotNull MultipartFile image, @NotNull UUID userId) {

        if (image.getSize() > MAX_SIZE) {
            throw new InvalidDataException("Image size cannot exceed 1.5MB");
        }

        var imgName = image.getOriginalFilename();
        if (imgName == null || !imgName.endsWith(ALLOWED_EXTENSION)) {
            throw new InvalidDataException("Image must be a "+ALLOWED_EXTENSION);
        }

        var s3 = AmazonS3ClientBuilder.defaultClient();

        var fileName = userId + ALLOWED_EXTENSION;
        var file = new File(fileName);
        try {
            Files.write(file.toPath(), image.getBytes());
            var result = s3.putObject(imageBucket, fileName, file);
            return result.getVersionId();

        } catch (IOException e) {
            throw new UnexpectedErrorException(e);

        } finally {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                LOGGER.error("[S3Service] Error deleting file:"+file.getAbsolutePath(), e);
            }
        }
    }

    public String getImagePath(UUID userId, String version) {
        var imgName = userId + ALLOWED_EXTENSION;
        return imageUrl.replace("{id}", imgName).replace("{version}", version);
    }

}
