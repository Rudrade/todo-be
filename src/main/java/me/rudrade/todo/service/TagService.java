package me.rudrade.todo.service;

import lombok.NonNull;
import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Tag save(@NonNull Tag tag) {
        return tagRepository.save(tag);
    }

    public List<Tag> findByUser(@NonNull User user) {
        return tagRepository.findByUserId(user.getId());
    }

    public Tag findOrCreateByUser(@NonNull User user, @NonNull Tag tag) {
        Optional<Tag> optionalTag = tagRepository.findByNameAndUserId(tag.getName(), user.getId());
        return optionalTag.orElseGet(() -> save(tag));
    }

    public void deleteById(UUID id) {
        tagRepository.deleteById(id);
    }

    public Optional<Tag> findByName(String name) {
        return tagRepository.findByName(name);
    }
}
