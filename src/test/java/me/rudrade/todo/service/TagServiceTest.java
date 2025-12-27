package me.rudrade.todo.service;

import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    private TagService tagService;
    private TagService getTagService() {
        if (tagService == null) {
            tagService = new TagService(tagRepository);
        }
        return tagService;
    }

    @Test
    void itShouldSave() {
        Tag tag = new Tag(null, "tag-test", "black", null, null);

        when(tagRepository.save(tag)).thenReturn(tag);

        Tag result = getTagService().save(tag);
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(tag);

        verify(tagRepository, times(1)).save(tag);
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldFindByUser() {
        User user = new User();
        user.setId(UUID.randomUUID());

        List<Tag> lst = new ArrayList<>();
        lst.add(new Tag(UUID.randomUUID(), "tag-test-1", "black", null, null));
        lst.add(new Tag(UUID.randomUUID(), "tag-test-2", "white", null, null));

        when(tagRepository.findByUserId(user.getId()))
            .thenReturn(lst);

        List<Tag> result = getTagService().findByUser(user);

        assertThat(result)
            .usingElementComparator(Comparator.comparing(Tag::getId))
            .containsExactlyInAnyOrderElementsOf(lst);

        verify(tagRepository, times(1)).findByUserId(user.getId());
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldThrowNullWhenFindingByNull() {
        assertThatThrownBy(() -> getTagService().findByUser(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void itShouldDelete() {
        UUID id = UUID.randomUUID();

        getTagService().deleteById(id);

        verify(tagRepository, times(1)).deleteById(id);
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldFindOrCreateByUserWhenExists() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Tag tag = new Tag(UUID.randomUUID(), "tag-test", "black", user, null);

        when(tagRepository.findByNameAndUserId(tag.getName(), user.getId()))
            .thenReturn(Optional.of(tag));

        Tag result = getTagService().findOrCreateByUser(user, tag);

        assertThat(result)
            .isEqualTo(tag);

        verify(tagRepository, times(1)).findByNameAndUserId(tag.getName(), user.getId());
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldFindOrCreateByUserWhenNotExists() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Tag tag = new Tag(null, "tag-test", "black", user, null);

        when(tagRepository.findByNameAndUserId(tag.getName(), user.getId()))
            .thenReturn(Optional.empty());

        when(tagRepository.save(tag))
            .thenReturn(tag);

        Tag result = getTagService().findOrCreateByUser(user, tag);

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(tag);

        verify(tagRepository, times(1)).findByNameAndUserId(tag.getName(), user.getId());
        verify(tagRepository, times(1)).save(tag);
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldFindOrCreateByUserWhenColorIsNull() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Tag tag = new Tag(null, "tag-test", null, null, null);

        when(tagRepository.findByNameAndUserId(tag.getName(), user.getId()))
            .thenReturn(Optional.empty());
        when(tagRepository.save(tag))
            .thenReturn(tag);

        Tag result = getTagService().findOrCreateByUser(user, tag);

        assertThat(tag.getColor())
            .matches("^#[0-9a-fA-F]{6}$");
        assertThat(tag.getUser())
            .isEqualTo(user);
        assertThat(result)
            .isEqualTo(tag);

        verify(tagRepository, times(1)).findByNameAndUserId(tag.getName(), user.getId());
        verify(tagRepository, times(1)).save(tag);
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldFindOrCreateByUserWhenColorIsEmpty() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Tag tag = new Tag(null, "tag-test", "", null, null);

        when(tagRepository.findByNameAndUserId(tag.getName(), user.getId()))
            .thenReturn(Optional.empty());
        when(tagRepository.save(tag))
            .thenReturn(tag);

        Tag result = getTagService().findOrCreateByUser(user, tag);

        assertThat(tag.getColor())
            .matches("^#[0-9a-fA-F]{6}$");
        assertThat(tag.getUser())
            .isEqualTo(user);
        assertThat(result)
            .isEqualTo(tag);

        verify(tagRepository, times(1)).findByNameAndUserId(tag.getName(), user.getId());
        verify(tagRepository, times(1)).save(tag);
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldGetByName() {
        String name = "tag-test";

        Tag tag = new Tag(UUID.randomUUID(), name, "black", null, null);

        when(tagRepository.findByName(name))
            .thenReturn(Optional.of(tag));

        Optional<Tag> result = getTagService().findByName(name);

        assertThat(result)
            .hasValue(tag);

        verify(tagRepository, times(1)).findByName(name);
        verifyNoMoreInteractions(tagRepository);
    }

}
