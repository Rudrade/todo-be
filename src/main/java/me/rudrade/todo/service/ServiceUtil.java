package me.rudrade.todo.service;

import java.util.concurrent.ThreadLocalRandom;

public class ServiceUtil {

    String generateRandomHexColor() {
        int nextInt = ThreadLocalRandom.current().nextInt(0xffffff + 1);
        return String.format("#%06x", nextInt);
    }
}
