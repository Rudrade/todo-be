package me.rudrade.todo.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import me.rudrade.todo.exception.InvalidDataException;

class S3ServiceTest {
    private static final String BUCKET_NAME = "s3-test-bucket";

    private S3Service target;

    @BeforeEach
    void setup() {
        target = new S3Service();
        ReflectionTestUtils.setField(target, "imageBucket", BUCKET_NAME);
    }

    @Test
    void itShouldUploadImage() throws IOException {
        var image = spy(MultipartFile.class);
        var id = UUID.randomUUID();

        when(image.getSize()).thenReturn(1048576L);
        when(image.getName()).thenReturn("image.webp");
        when(image.getBytes()).thenReturn(new byte[1024]);

        try (
            var s3Builder = mockStatic(AmazonS3ClientBuilder.class);
            var files = mockStatic(Files.class);
        ) {
            var s3 = mock(AmazonS3.class);
            s3Builder.when(AmazonS3ClientBuilder::defaultClient).thenReturn(s3);

            target.uploadImage(image, id);

            s3Builder.verify(AmazonS3ClientBuilder::defaultClient, times(1));
            s3Builder.verifyNoMoreInteractions();

            files.verify(() -> Files.write(any(Path.class), any(byte[].class)), times(1));
            files.verify(() -> Files.deleteIfExists(any(Path.class)), times(1));
            files.verifyNoMoreInteractions();

            verify(s3, times(1)).putObject(eq(BUCKET_NAME), eq(id+".webp"), any(File.class));
            verifyNoMoreInteractions(s3);
        }
    }

    @Test
    void itShouldThrowWhenExceedsSize() {
        var image = spy(MultipartFile.class);
        var userId = UUID.randomUUID();

        when(image.getSize()).thenReturn(1672864L);

        assertThrows(InvalidDataException.class, () -> target.uploadImage(image, userId));
    }

    @Test
    void itShouldThrowWhenIsNotWebp() {
        var image = spy(MultipartFile.class);
        var userId = UUID.randomUUID();

        when(image.getSize()).thenReturn(1072864L);
        when(image.getName()).thenReturn("image.png");

        assertThrows(InvalidDataException.class, () -> target.uploadImage(image, userId));
    }

}
