package me.rudrade.todo.util;

import java.util.concurrent.ThreadLocalRandom;

public class ServiceUtil {

    public String generateRandomHexColor() {
        int nextInt = ThreadLocalRandom.current().nextInt(0xffffff + 1);
        return String.format("#%06x", nextInt);
    }
}
