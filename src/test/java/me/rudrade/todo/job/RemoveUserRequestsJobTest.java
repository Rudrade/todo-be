package me.rudrade.todo.job;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.rudrade.todo.repository.PasswordRequestRepository;
import me.rudrade.todo.repository.UserRequestRepository;

@ExtendWith(MockitoExtension.class)
class RemoveUserRequestsJobTest {

    @Mock private UserRequestRepository userRequestRepository;
    @Mock private PasswordRequestRepository passwordRequestRepository;

    private RemoveUserRequestsJob target;

    @BeforeEach
    void setup() {
        target = new RemoveUserRequestsJob(userRequestRepository, passwordRequestRepository);
    }

    @Test
    void itShouldRunJob() {
        target.job();

        verify(userRequestRepository, times(1)).deleteIfExpired(60);
        verify(passwordRequestRepository, times(1)).deleteIfExpired(60);
        verifyNoMoreInteractions(userRequestRepository, passwordRequestRepository);
    }

}
