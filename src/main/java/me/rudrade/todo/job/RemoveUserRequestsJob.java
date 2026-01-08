package me.rudrade.todo.job;

import lombok.AllArgsConstructor;
import me.rudrade.todo.repository.PasswordRequestRepository;
import me.rudrade.todo.repository.UserRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class RemoveUserRequestsJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveUserRequestsJob.class);

    private static final int EXPIRATION_MINUTES = 60;

    private final UserRequestRepository userRequestRepository;
    private final PasswordRequestRepository passwordRequestRepository;

    //@Scheduled(cron = "0 0 8-20 ? * MON-FRI")
    @Transactional
    void job() {
        LOGGER.info("[RemoveUserRequestsJob] Starting Job");

        deleteUserRequests();

        deletePasswordRequests();

        LOGGER.info("[RemoveUserRequestsJob] End Job");
    }
    
    private void deleteUserRequests() {
        LOGGER.info("[RemoveUserRequestsJob] Started removing user requests");
        userRequestRepository.deleteIfExpired(EXPIRATION_MINUTES);
        LOGGER.info("[RemoveUserRequestsJob] Removed user requests");
    }

    private void deletePasswordRequests() {
        LOGGER.info("[RemoveUserRequestsJob] Started removing password requests");
        passwordRequestRepository.deleteIfExpired(EXPIRATION_MINUTES);
        LOGGER.info("[RemoveUserRequestsJob] Removed password requests");
    }
}
