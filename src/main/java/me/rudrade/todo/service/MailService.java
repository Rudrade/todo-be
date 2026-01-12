package me.rudrade.todo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import me.rudrade.todo.model.PasswordRequest;
import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.repository.PasswordRequestRepository;
import me.rudrade.todo.repository.UserRequestRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final UserRequestRepository userRequestRepository;
    private final PasswordRequestRepository passwordRequestRepository;
    private final MessageSource messageSource;

    @Value("${todo.app.mail.activationUrl}")
    private String activationUrl;

    @Value("${todo.app.mail.resetPasswordUrl}")
    private String resetPasswordUrl;

    @Value("${profile.active}")
    private String profile;
    
    public boolean sendActivationMail(UserRequest user) {
        if (activationUrl == null || !activationUrl.contains("{id}"))
            throw new IllegalStateException("Activation url is miss configured");

        try {
            var url = activationUrl.replace("{id}", user.getId().toString());

            var message = messageSource.getMessage("mail.activation.body", null, user.getLocale());
            message = message.replace("${username}", user.getUsername());
            message = message.replace("${url}", url);

            var subject = getAppSubject() + messageSource.getMessage("mail.activation.subject", null, user.getLocale());

            send(user.getEmail(), subject, message);

            user.setMailSent(true);
            userRequestRepository.save(user);
            return true;
        } catch (Exception e) {
            LOGGER.error("[MailService.sendActivationMail] ", e);
            return false;
        }
    }

    public boolean sendPasswordReset(@NotNull PasswordRequest request) {
        if (resetPasswordUrl == null || !resetPasswordUrl.contains("{id}"))
            throw new IllegalStateException("Password reset url is miss configured");

        try {
            var locale = request.getUser().getLocale();

            var url = resetPasswordUrl.replace("{id}", request.getId().toString());

            var message = messageSource.getMessage("mail.passwordReset.body", null, locale);
            message = message.replace("${username}", request.getUser().getUsername());
            message = message.replace("${url}", url);

            var subject = getAppSubject() + messageSource.getMessage("mail.passwordReset.subject", null, locale);

            send(request.getUser().getEmail(), subject, message);

            request.setMailSent(true);
            passwordRequestRepository.save(request);

            return true;
        } catch (Exception e) {
            LOGGER.error("[MailService.sendPasswordReset] ", e);
            return false;
        }
    }

    private String getAppSubject() {
        if ("dev".equalsIgnoreCase(profile)) {
            return "[DEV] TodoApp ";
        } else if ("local".equalsIgnoreCase(profile)) {
            return "[LOCAL] TodoApp ";
        }

        return "";
    }

    public void send(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);

        mailSender.send(message);
    }
}
