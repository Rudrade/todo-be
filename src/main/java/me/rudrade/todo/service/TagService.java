package me.rudrade.todo.service;

import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.User;
import me.rudrade.todo.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TagService extends ServiceUtil {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Tag save(Tag tag) {
        return tagRepository.save(tag);
    }

    public List<Tag> findByUser(User user) {
        return tagRepository.findByUserId(user.getId());
    }

    public Tag findOrCreateByUser(User user, Tag tag) {
        Optional<Tag> optionalTag = tagRepository.findByNameAndUserId(tag.getName(), user.getId());
        return optionalTag.orElseGet(() -> {
            if (tag.getColor() == null || tag.getColor().isEmpty()) {
                tag.setColor(generateRandomHexColor());
                tag.setUser(user);
            }
          return save(tag);
        });
    }

    public void deleteById(UUID id) {
        tagRepository.deleteById(id);
    }

    public Optional<Tag> findByName(String name) {
        return tagRepository.findByName(name);
    }
}
