package me.rudrade.todo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import me.rudrade.todo.config.ConfigurationUtil;
import me.rudrade.todo.config.SqlIntegrationTest;
import me.rudrade.todo.model.PasswordRequest;

@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class))
@Import({ConfigurationUtil.PasswordEncoder.class, ConfigurationUtil.MailSender.class})
class PasswordRequestRepositoryTest extends SqlIntegrationTest {

    @Autowired private PasswordRequestRepository target;

    @Test
    void itShouldDeleteIfExpired() { 
        var request = data();
        target.save(request);

        var requestDelete = data();
        requestDelete.setDtCreated(LocalDateTime.now().minusDays(1L));
        target.save(requestDelete);

        target.deleteIfExpired(60);

        var dbDeleted = target.findById(requestDelete.getId());
        assertThat(dbDeleted).isEmpty();
    }

    @Test
    void itShouldFindAllByMailSentIsFalse() {
        var request1 = data();
        target.save(request1);

        var request2 = data();
        target.save(request2);

        var request3 = data();
        request3.setMailSent(true);
        target.save(request3);

        var result = target.findAllByMailSentIsFalse();
        assertThat(result)
            .hasSize(2)
            .usingElementComparator(Comparator.comparing(PasswordRequest::getId))
            .containsExactlyInAnyOrder(request1, request2);
    }

    private PasswordRequest data() {
        var data = new PasswordRequest();
        data.setDtCreated(LocalDateTime.now());
        data.setUser(getTestUser());
        return data;
    }

}
