package me.rudrade.todo.util;

import java.util.concurrent.ThreadLocalRandom;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;

public class ServiceUtil {

    public String generateRandomHexColor() {
        int nextInt = ThreadLocalRandom.current().nextInt(0xffffff + 1);
        return String.format("#%06x", nextInt);
    }

    public void validate(@NotNull Object obj) {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();

        var result = validator.validate(obj);
        if (result != null && !result.isEmpty()) {
            throw new ConstraintViolationException(result);
        }
    }

    public static String trimString(String str) {
        if (str == null || str.isBlank()) return null;

        return str.trim();
    }
}
