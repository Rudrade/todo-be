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

            var message = new StringBuilder();
            message.append("Dear ").append(user.getUsername()).append(",\n\n");
            message.append("Please activate your account via the url: ").append(url).append("\n\n");
            message.append("This activation link is available for about 1 hour.");

            send(user.getEmail(), getAppSubject()+"- Account activation", message.toString());

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
            var url = resetPasswordUrl.replace("{id}", request.getId().toString());

            var message = new StringBuilder();
            message.append("Dear ").append(request.getUser().getUsername()).append(",\n\n");
            message.append("Please follow the url to reset your password: ").append(url).append("\n\n");
            message.append("This link is available for about 1 hour.");

            send(request.getUser().getEmail(), getAppSubject()+"- Password reset", message.toString());

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
