package me.rudrade.todo.job;

import me.rudrade.todo.model.PasswordRequest;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.types.Role;
import me.rudrade.todo.repository.PasswordRequestRepository;
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
    @Mock private PasswordRequestRepository passwordRequestRepository;

    private SendMissingMailsJob target;

    @BeforeEach
    void setup() {
        target = new SendMissingMailsJob(userRequestRepository, mailService, passwordRequestRepository);
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

    private PasswordRequest passwordRequest() {
        var user = new User();
        user.setId(UUID.randomUUID());

        var request = new PasswordRequest();
        request.setDtCreated(LocalDateTime.now());
        request.setId(UUID.randomUUID());
        request.setUser(user);
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

        verify(passwordRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(passwordRequestRepository);
    }

    @Test
    void itShouldSendMissingPasswordRequests() {
        var request1 = passwordRequest();
        var request2 = passwordRequest();

        when(passwordRequestRepository.findAllByMailSentIsFalse()).thenReturn(List.of(request1, request2));

        target.job();

        verify(userRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(userRequestRepository);

        verify(passwordRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(passwordRequestRepository);

        verify(mailService, times(1)).sendPasswordReset(request1);
        verify(mailService, times(1)).sendPasswordReset(request2);
        verifyNoMoreInteractions(mailService);
    }

    @Test
    void itShouldStopIfIsMissingConfigurationOnActivation() {
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
    void itShouldStopIfMissingConfigurationOnPassword() {
        var request1 = passwordRequest();
        var request2 = passwordRequest();

        when(passwordRequestRepository.findAllByMailSentIsFalse()).thenReturn(List.of(request1, request2));

        doThrow(new IllegalStateException())
            .when(mailService)
            .sendPasswordReset(request1);

        target.job();

        verify(userRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(userRequestRepository);

        verify(passwordRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(passwordRequestRepository);

        verify(mailService, times(1)).sendPasswordReset(request1);
        verifyNoMoreInteractions(mailService);
    }


    @Test
    void itShouldCointinueIfExceptionIsThrownOnPassword() {
        var request1 = passwordRequest();
        var request2 = passwordRequest();

        when(passwordRequestRepository.findAllByMailSentIsFalse()).thenReturn(List.of(request1, request2));

        doThrow(new NullPointerException())
            .when(mailService)
            .sendPasswordReset(request1);

        target.job();

        verify(userRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(userRequestRepository);

        verify(passwordRequestRepository, times(1)).findAllByMailSentIsFalse();
        verifyNoMoreInteractions(passwordRequestRepository);

        verify(mailService, times(1)).sendPasswordReset(request1);
        verify(mailService, times(1)).sendPasswordReset(request2);
        verifyNoMoreInteractions(mailService);
    }

    @Test
    void itShouldContinueIfExceptionIsThrownOnActivation() {
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

