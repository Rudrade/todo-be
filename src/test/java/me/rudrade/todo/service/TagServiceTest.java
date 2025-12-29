package me.rudrade.todo.service;

import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void itShouldThrowWhenFindingByNullUser() {
        TagService service = getTagService();

        assertThrows(InvalidAccessException.class, () -> service.findByUser(null));
        verifyNoInteractions(tagRepository);
    }

    @Test
    void itShouldThrowWhenFindingByUserWithoutId() {
        User user = new User();
        TagService service = getTagService();

        assertThrows(InvalidAccessException.class, () -> service.findByUser(user));
        verifyNoInteractions(tagRepository);
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
    void itShouldThrowWhenFindOrCreateWithNullUser() {
        Tag tag = new Tag();
        TagService service = getTagService();

        assertThrows(InvalidAccessException.class, () -> service.findOrCreateByUser(null, tag));
        verifyNoInteractions(tagRepository);
    }

    @Test
    void itShouldThrowWhenFindOrCreateWithNullTag() {
        User user = new User();
        user.setId(UUID.randomUUID());
        TagService service = getTagService();

        assertThrows(InvalidDataException.class, () -> service.findOrCreateByUser(user, null));
        verifyNoInteractions(tagRepository);
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
    void itShouldFindByName() {
        User user = new User();
        user.setId(UUID.randomUUID());
        Tag tag = new Tag(UUID.randomUUID(), "tag-test", "black", user, null);
        when(tagRepository.findByNameAndUserId(tag.getName(), user.getId()))
            .thenReturn(Optional.of(tag));

        Optional<Tag> result = getTagService().findByName(tag.getName(), user);

        assertThat(result).contains(tag);
        verify(tagRepository, times(1)).findByNameAndUserId(tag.getName(), user.getId());
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldThrowWhenFindingByNameWithInvalidArgs() {
        User userWithoutId = new User();
        User validUser = new User();
        validUser.setId(UUID.randomUUID());
        TagService service = getTagService();

        assertThrows(InvalidAccessException.class, () -> service.findByName(null, validUser));
        assertThrows(InvalidAccessException.class, () -> service.findByName("   ", validUser));
        assertThrows(InvalidAccessException.class, () -> service.findByName("tag", null));
        assertThrows(InvalidAccessException.class, () -> service.findByName("tag", userWithoutId));

        verifyNoInteractions(tagRepository);
    }

    @Test
    void itShouldDeleteWhenOwned() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());
        Tag tag = new Tag(id, "tag", "color", user, null);
        when(tagRepository.findByIdAndUserId(id, user.getId()))
            .thenReturn(Optional.of(tag));

        getTagService().deleteById(id, user);

        verify(tagRepository, times(1)).findByIdAndUserId(id, user.getId());
        verify(tagRepository, times(1)).deleteById(id);
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldThrowWhenDeletingNotOwned() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());
        when(tagRepository.findByIdAndUserId(id, user.getId()))
            .thenReturn(Optional.empty());
        TagService service = getTagService();

        assertThrows(InvalidAccessException.class, () -> service.deleteById(id, user));

        verify(tagRepository, times(1)).findByIdAndUserId(id, user.getId());
        verify(tagRepository, never()).deleteById(any());
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void itShouldThrowWhenDeletingWithInvalidArgs() {
        UUID id = UUID.randomUUID();
        User userNoId = new User();
        TagService service = getTagService();

        assertThrows(InvalidAccessException.class, () -> service.deleteById(null, userNoId));
        assertThrows(InvalidAccessException.class, () -> service.deleteById(id, null));
        assertThrows(InvalidAccessException.class, () -> service.deleteById(id, userNoId));

        verifyNoInteractions(tagRepository);
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

}
