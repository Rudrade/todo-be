package me.rudrade.todo.job;

import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.UserRequestRepository;
import me.rudrade.todo.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendMissingConfirmationMailsJobTest {

    @Mock private UserRequestRepository userRequestRepository;
    @Mock private MailService mailService;

    private SendMissingConfirmationMailsJob target;

    @BeforeEach
    void setup() {
        target = new SendMissingConfirmationMailsJob(userRequestRepository, mailService);
    }

    private UserRequest userRequest() {
        var id = UUID.randomUUID();
        var request = new UserRequest();
        request.setId(id);
        request.setUsername(id+"-user");
        request.setPassword("test");
        request.setEmail(id+"@test");
        request.setRole(Role.ROLE_USER);
        request.setDtCreated(LocalDateTime.now());
        request.setMailSent(false);
        return request;
    }

    @Test
    void itShouldSendActivationMail() {
        var userRequest1 = userRequest();
        var userRequest2 = userRequest();

        when(userRequestRepository.findAllByMailSentIsFalse())
            .thenReturn(List.of(userRequest1, userRequest2));

        target.job();

        verify(userRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(userRequestRepository);

        verify(mailService, times(1)).sendActivationMail(userRequest1);
        verify(mailService, times(1)).sendActivationMail(userRequest2);
        verifyNoMoreInteractions(mailService);
    }

    @Test
    void itShouldStopIfIsMissingConfiguration() {
        var userRequest1 = userRequest();
        var userRequest2 = userRequest();

        when(userRequestRepository.findAllByMailSentIsFalse())
            .thenReturn(List.of(userRequest1, userRequest2));

        doThrow(new IllegalStateException("Activation url is miss configured"))
            .when(mailService)
            .sendActivationMail(any(UserRequest.class));

        target.job();

        verify(userRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(userRequestRepository);

        verify(mailService, times(1)).sendActivationMail(any(UserRequest.class));
        verifyNoMoreInteractions(mailService);
    }

    @Test
    void itShouldContinueIfExceptionIsThrown() {
        var userRequest1 = userRequest();
        var userRequest2 = userRequest();

        when(userRequestRepository.findAllByMailSentIsFalse())
            .thenReturn(List.of(userRequest1, userRequest2));

        doThrow(new RuntimeException("unexpected")).when(mailService).sendActivationMail(userRequest1);

        target.job();

        verify(userRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(userRequestRepository);

        verify(mailService, times(1)).sendActivationMail(userRequest1);
        verify(mailService, times(1)).sendActivationMail(userRequest2);
        verifyNoMoreInteractions(mailService);
    }
}

