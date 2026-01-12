package me.rudrade.todo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import me.rudrade.todo.dto.TagDto;
import me.rudrade.todo.dto.filter.TaskListFilter;
import me.rudrade.todo.dto.response.TaskListResponse;
import me.rudrade.todo.model.Tag;
import me.rudrade.todo.model.User;
import me.rudrade.todo.model.UserList;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.repository.TaskRepository;
import me.rudrade.todo.util.ServiceUtil;
import me.rudrade.todo.dto.filter.TaskListFilter.Filter;

@Service
@RequiredArgsConstructor
public class TaskService extends ServiceUtil {

	private final UserListService userListService;
	private final TaskRepository repository;
	private final TagService tagService;
	private final MessageSource messageSource;

	public TaskDto saveTask(TaskDto input, User user) {

		if (input.getId() != null) {
			Optional<Task> optTask = repository.findByIdAndUserId(input.getId(), user.getId());
			if (optTask.isEmpty()) {
				throw new InvalidDataException(messageSource.getMessage("task.missing", null, user.getLocale()));
			}
		}

		Task inputTask = Mapper.toTask(input);
		inputTask.setUser(user);
		if (input.getListName() != null) {
			if (input.getListName().isBlank()) {
				inputTask.setUserList(null);
			} else {
				UserList userList = userListService.saveByName(input.getListName(), user);
				inputTask.setUserList(userList);
			}
		}

		// Find all tasks by name and user
		List<Tag> lstTags = null;
		if (input.getTags() != null && !input.getTags().isEmpty()) {
			// If missing some, create them
			lstTags = new ArrayList<>();
			for (TagDto tag : input.getTags()) {
				lstTags.add(tagService.findOrCreateByUser(user, Mapper.toTag(tag)));
			}

		}
		// Associate final list of tags to task
		inputTask.setTags(lstTags);

		Task task = repository.save(inputTask);
		return Mapper.toTaskDto(task);
	}
	
	@Transactional(readOnly = true)
	public TaskListResponse getAll(TaskListFilter filter) {
		validateFilter(filter);

		Page<Task> result;
		UUID userId = filter.user().getId();
        if (Filter.TODAY.equals(filter.filter())) {
            result = repository.findDueToday(userId, Pageable.unpaged());

        } else if (Filter.UPCOMING.equals(filter.filter())) {
            result = repository.findDueUpcoming(userId, Pageable.unpaged());

        } else if (Filter.SEARCH.equals(filter.filter())) {
			result = repository.findByTitleContains(filter.searchTerm(), userId, Pageable.unpaged());

		} else if (Filter.LIST.equals(filter.filter())) {
			result = repository.findAllByUserListNameAndUserId(filter.searchTerm(), userId, Pageable.unpaged());

		} else if (Filter.TAG.equals(filter.filter())) {
			result = repository.findAllByTagsNameAndUserId(filter.searchTerm(), userId, Pageable.unpaged());

        } else {
            result = repository.findAllByUserId(userId, Pageable.unpaged());
        }

		return new TaskListResponse(
			result.getTotalElements(),
			result.map(Mapper::toTaskDto).toList()
		);
	}

	private void validateFilter(TaskListFilter filter) {
		if (filter == null || filter.user() == null || filter.user().getId() == null)
			throw new InvalidAccessException();

		if ((filter.searchTerm() == null || filter.searchTerm().isBlank()) && (
			Filter.SEARCH.equals(filter.filter()) ||
			Filter.LIST.equals(filter.filter()) ||
			Filter.TAG.equals(filter.filter())
		)) {
			throw new InvalidDataException(messageSource.getMessage("searchTerm.missing", null, filter.user().getLocale()));
		}
	}
	
	public TaskDto getById(UUID id, User user) {
		if (id == null || user == null || user.getId() == null)
			throw new InvalidAccessException();

		Optional<Task> optTask = repository.findByIdAndUserId(id, user.getId());
		if (optTask.isEmpty()) {
			throw new InvalidDataException(messageSource.getMessage("task.missing", null, user.getLocale()));
		}
		
		return Mapper.toTaskDto(optTask.get());
	}
	
	public void deleteById(UUID id, User user) {
		if (id == null || user == null || user.getId() == null)
			throw new InvalidAccessException();

		Optional<Task> optTask = repository.findByIdAndUserId(id, user.getId());
		if (optTask.isEmpty()) {
			throw new InvalidDataException(messageSource.getMessage("task.missing", null, user.getLocale()));
		}
		
		repository.deleteById(id);
	}
	
}
