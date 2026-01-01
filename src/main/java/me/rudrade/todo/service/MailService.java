package me.rudrade.todo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import me.rudrade.todo.model.UserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired private JavaMailSender mailSender;

    @Value("${todo.app.mail.activationUrl}")
    private String activationUrl;

    public void sendActivationMail(UserRequest user) throws MessagingException {
        if (activationUrl == null || !activationUrl.contains("{id}"))
            throw new IllegalStateException("Activation url is miss configured");

        var url = activationUrl.replace("{id}", user.getId().toString());

        var message = new StringBuilder();
        message.append("Dear ").append(user.getUsername()).append(",\n\n");
        message.append("Please activate your account via the url: ").append(url).append("\n\n");
        message.append("This activation link is available for about 1 hour.");

        send(user.getEmail(), "TodoApp - Account activation", message.toString());
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
