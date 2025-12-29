package me.rudrade.todo.controller;

import me.rudrade.todo.config.ControllerIntegration;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.TagDto;
import me.rudrade.todo.dto.response.TagListResponse;
import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

class TagControllerTest extends ControllerIntegration {

    private static final String BASE_URI = "/todo/api/tag";

    @Autowired private MockMvcTester mvc;
    @Autowired private TagRepository tagRepository;

    @Test
    void itShouldGetAllTags() {
        User user = getTestUser();

        Tag tag1 = new Tag(null, "tag-test-1", "black", user, null);
        Tag tag2 = new Tag(null, "tag-test-2", "red", user, null);

        tagRepository.save(tag1);
        tagRepository.save(tag2);

        assertThat(mvc.get().uri(BASE_URI)
            .headers(getAuthHeader()))
            .hasStatusOk()
            .bodyJson()
            .convertTo(TagListResponse.class)
            .satisfies(response ->
                assertThat(response.tags())
                    .hasSize(2)
                    .usingElementComparator(Comparator.comparing(TagDto::name).thenComparing(TagDto::color))
                    .containsExactlyInAnyOrder(Mapper.toTagDto(tag1), Mapper.toTagDto(tag2))
            );
    }

    @Test
    void itShouldDeleteTag() {
        var user = getTestUser();

        Tag tag1 = new Tag(null, "tag-test-1", "black", user, null);
        tagRepository.save(tag1);

        assertThat(mvc.delete().uri(BASE_URI + "/{id}", tag1.getId())
            .headers(getAuthHeader()))
            .hasStatusOk();

        assertThat(tagRepository.findById(tag1.getId())).isEmpty();
    }

}
