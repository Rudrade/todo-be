package me.rudrade.todo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import me.rudrade.todo.model.UserRequest;
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

    @Value("${todo.app.mail.activationUrl}")
    private String activationUrl;

    public void sendActivationMail(UserRequest user) {
        if (activationUrl == null || !activationUrl.contains("{id}"))
            throw new IllegalStateException("Activation url is miss configured");

        try {
            var url = activationUrl.replace("{id}", user.getId().toString());

            var message = new StringBuilder();
            message.append("Dear ").append(user.getUsername()).append(",\n\n");
            message.append("Please activate your account via the url: ").append(url).append("\n\n");
            message.append("This activation link is available for about 1 hour.");

            send(user.getEmail(), "TodoApp - Account activation", message.toString());

            user.setMailSent(true);
            userRequestRepository.save(user);
        } catch (Exception e) {
            LOGGER.error("[MailService.sendActivationMail] ", e);
        }
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
