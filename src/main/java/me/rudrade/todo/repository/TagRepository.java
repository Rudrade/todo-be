package me.rudrade.todo.repository;

import me.rudrade.todo.model.Tag;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends CrudRepository<Tag, UUID> {

    List<Tag> findByUserId(UUID userId);

    Optional<Tag> findByNameAndUserId(String name, UUID userId);

    Optional<Tag> findByName(String name);
}
