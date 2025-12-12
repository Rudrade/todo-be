package me.rudrade.todo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@RestController
@RequestMapping("/health")
public class HealthController {

    private static final Logger LOGGER = Logger.getLogger(HealthController.class.getName());

    @GetMapping
    public String checkHealth() {
        LOGGER.info("Health check");
        return "OK";
    }
}
