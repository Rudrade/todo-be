package me.rudrade.todo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.rudrade.todo.model.Task;

@Repository
public interface TaskRepository extends CrudRepository<Task, UUID>{

    @Query(value = "SELECT t FROM Task t WHERE t.dueDate = CURRENT_DATE AND t.user.id = ?1",
        countQuery = "SELECT count(t.id) FROM Task t WHERE t.dueDate = CURRENT_DATE AND t.user.id = ?1"
    )
    Page<Task> findDueToday(UUID userId, Pageable pageable);

    @Query(value = "SELECT t FROM Task t WHERE t.dueDate > CURRENT_DATE AND t.user.id = ?1",
        countQuery = "SELECT count(t.id) FROM Task t WHERE t.dueDate > CURRENT_DATE AND t.user.id = ?1"
    )
    Page<Task> findDueUpcoming(UUID userId, Pageable pageable);

    @Query(value = "SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', ?1, '%')) AND t.user.id = ?2",
        countQuery = "SELECT count(t.id) FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', ?1, '%')) AND t.user.id = ?2"
    )
    Page<Task> findByTitleContains(String title, UUID userId, Pageable pageable);

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    Page<Task> findAllByUserId(UUID userId, Pageable pageable);

    Page<Task> findAllByUserListNameAndUserId(String name, UUID userId, Pageable pageable);

    @NativeQuery(value = "select distinct t.* from task t inner join tag_task tt on tt.task_id = t.id inner join tag tg on tg.id = tt.tag_id where tg.name = ?1 and t.user_id = ?2",
        countQuery = "select count(distinct(t.id)) from task t inner join tag_task tt on tt.task_id = t.id inner join tag tg on tg.id = tt.tag_id where tg.name = ?1 and t.user_id = ?2"
    )
    Page<Task> findAllByTagsNameAndUserId(String name, UUID userId, Pageable pageable);
}
