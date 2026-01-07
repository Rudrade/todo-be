package me.rudrade.todo.job;

import lombok.AllArgsConstructor;
import me.rudrade.todo.repository.UserRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class RemoveUserRequestsJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveUserRequestsJob.class);

    private static final int EXPIRATION_MINUTES = 60;

    private final UserRequestRepository userRequestRepository;

    //@Scheduled(cron = "0 0 8-20 ? * MON-FRI")
    @Transactional
    void job() {
        LOGGER.info("[RemoveUserRequestsJob] Starting Job");

        // Remove UserRequests that passes minutesExpiration
        userRequestRepository.deleteIfExpired(EXPIRATION_MINUTES);

        LOGGER.info("[RemoveUserRequestsJob] End Job");
    }
}
