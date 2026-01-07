package me.rudrade.todo.job;

import lombok.RequiredArgsConstructor;
import me.rudrade.todo.repository.UserRequestRepository;
import me.rudrade.todo.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendMissingConfirmationMailsJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendMissingConfirmationMailsJob.class);

    private final UserRequestRepository userRequestRepository;
    private final MailService mailService;

    //@Scheduled(cron = "0 */5 * * * *")
    void job () {
        LOGGER.info("[SendMissingConfirmationMailsJob] Starting Job");

        // Get missing mails
        var lstRequests = userRequestRepository.findAllByMailSentIsFalse();

        // Send
        for (var request : lstRequests) {
            try {
                mailService.sendActivationMail(request);
            } catch (IllegalStateException ex) {
                LOGGER.error("[SendMissingConfirmationMailsJob.job] ", ex);
                break;

            } catch (Exception ex) {
                LOGGER.error("[SendMissingConfirmationMailsJob.job] ", ex);
            }
        }

        LOGGER.info("[SendMissingConfirmationMailsJob] End Job");
    }

}
