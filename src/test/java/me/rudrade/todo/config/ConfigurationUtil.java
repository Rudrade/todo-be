package me.rudrade.todo.config;

import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.mockito.Mockito.*;

public class ConfigurationUtil {

    @TestConfiguration
    public static class PasswordEncoder {
        @Bean
        org.springframework.security.crypto.password.PasswordEncoder getPasswordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @TestConfiguration
    public static class MailSender {
        @Bean
        JavaMailSender javaMailSender() {
            var mocked = mock(JavaMailSender.class) ;

            when(mocked.createMimeMessage())
                .thenReturn(mock(MimeMessage.class));

            return mocked;
        }
    }

}
