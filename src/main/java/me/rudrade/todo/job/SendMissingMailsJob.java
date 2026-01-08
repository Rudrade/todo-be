package me.rudrade.todo.job;

import lombok.RequiredArgsConstructor;
import me.rudrade.todo.repository.PasswordRequestRepository;
import me.rudrade.todo.repository.UserRequestRepository;
import me.rudrade.todo.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendMissingMailsJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendMissingMailsJob.class);

    private final UserRequestRepository userRequestRepository;
    private final MailService mailService;
    private final PasswordRequestRepository passwordRequestRepository;

    //@Scheduled(cron = "0 */5 * * * *")
    void job () {
        LOGGER.info("[SendMissingMailsJob] Starting Job");

        sendMissingActivations();

        sendMissingPasswordRequests();

        LOGGER.info("[SendMissingMailsJob] End Job");
    }

    private void sendMissingActivations() {
        LOGGER.info("[SendMissingMailsJob] Starting sending missing activation");

        // Get missing mails
        var lstRequests = userRequestRepository.findAllByMailSentIsFalse();

        // Send
        for (var request : lstRequests) {
            try {
                mailService.sendActivationMail(request);
            } catch (IllegalStateException ex) {
                LOGGER.error("[SendMissingConfirmationMailsJob.sendMissingActivations] ", ex);
                break;

            } catch (Exception ex) {
                LOGGER.error("[SendMissingConfirmationMailsJob.sendMissingActivations] ", ex);
            }
        }

        LOGGER.info("[SendMissingMailsJob] Done sending missing activation");
    }

    private void sendMissingPasswordRequests() {
        LOGGER.info("[SendMissingMailsJob] Starting sending missing password requests");

        var lstRequests = passwordRequestRepository.findAllByMailSentIsFalse();

        for (var request : lstRequests) {
            try {
                mailService.sendPasswordReset(request);
            } catch (IllegalStateException ex) {
                LOGGER.error("[SendMissingConfirmationMailsJob.sendMissingPasswordRequests] ", ex);
                break;

            } catch (Exception ex) {
                LOGGER.error("[SendMissingConfirmationMailsJob.sendMissingPasswordRequests] ", ex);
            }
        }

        LOGGER.info("[SendMissingMailsJob] Starting sending missing password requests");
    }

}
