package me.rudrade.todo.job;

import lombok.AllArgsConstructor;
import me.rudrade.todo.repository.UserRequestRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class RemoveUserRequestsJob {
    private static final int EXPIRATION_MINUTES = 60;

    private final UserRequestRepository userRequestRepository;

    @Scheduled(cron = "0 0 8-20 ? * MON-FRI")
    @Transactional
    void job() {
        // Remove UserRequests that passes minutesExpiration
        userRequestRepository.deleteIfExpired(EXPIRATION_MINUTES);
    }
}
