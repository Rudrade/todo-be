package me.rudrade.todo.service;

import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.TagRepository;
import me.rudrade.todo.util.ServiceUtil;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagService extends ServiceUtil {

    private final TagRepository tagRepository;
    private final MessageSource messageSource;

    public List<Tag> findByUser(User user) {
        if (user == null || user.getId() == null)
            throw new InvalidAccessException();

        return tagRepository.findByUserId(user.getId());
    }

    public Tag findOrCreateByUser(User user, Tag tag) {
        if (user == null) {
            throw new InvalidAccessException();
        }

        if (tag == null)
            throw new InvalidDataException(messageSource.getMessage("tag.missing", null, user.getLocale()));

        Optional<Tag> optionalTag = findByName(tag.getName(), user);
        return optionalTag.orElseGet(() -> {
            if (tag.getColor() == null || tag.getColor().isBlank()) {
                tag.setColor(generateRandomHexColor());
                tag.setUser(user);
            }
          return tagRepository.save(tag);
        });
    }

    public void deleteById(UUID id, User user) {
        if (id == null || user == null || user.getId() == null)
            throw new InvalidAccessException();

        Optional<Tag> optionalTag = tagRepository.findByIdAndUserId(id, user.getId());
        if (optionalTag.isEmpty())
            throw new InvalidAccessException();

        tagRepository.deleteById(id);
    }

    public Optional<Tag> findByName(String name, User user) {
        if (name == null || name.isBlank() || user == null || user.getId() == null)
            throw new InvalidAccessException();

        return tagRepository.findByNameAndUserId(name, user.getId());
    }
}
