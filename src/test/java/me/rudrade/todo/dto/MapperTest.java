package me.rudrade.todo.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import me.rudrade.todo.model.UserRequest;
import me.rudrade.todo.model.types.Role;

class MapperTest {

    @Test
    void itShouldToRequestDto() {
        var request = new UserRequest();
        request.setDtCreated(LocalDateTime.now());
        request.setEmail("test@test");
        request.setId(UUID.randomUUID());
        request.setMailSent(true);
        request.setPassword("test");
        request.setRole(Role.ROLE_USER);
        request.setUsername("test");

        var result = Mapper.toRequestDto(request);
        assertThat(result)
            .isNotNull()
            .satisfies(dto -> {
                assertThat(dto.id()).isEqualTo(request.getId());
                assertThat(dto.username()).isEqualTo(request.getUsername());
                assertThat(dto.email()).isEqualTo(request.getEmail());
                assertThat(dto.dtCreated()).isEqualTo(request.getDtCreated());
                assertThat(dto.mailSent()).isEqualTo(request.isMailSent());
            });
    }

}
