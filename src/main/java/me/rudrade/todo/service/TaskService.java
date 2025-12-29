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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.exception.InvalidDataException;
import me.rudrade.todo.exception.TaskNotFoundException;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.repository.TaskRepository;
import me.rudrade.todo.util.ServiceUtil;
import me.rudrade.todo.dto.filter.TaskListFilter.Filter;

@Service
public class TaskService extends ServiceUtil {

	private final UserListService userListService;
	private final TaskRepository repository;
	private final TagService tagService;

	public TaskService(UserListService userListService, TaskRepository repository, TagService tagService) {
		this.userListService = userListService;
		this.repository = repository;
		this.tagService = tagService;
	}

	public TaskDto saveTask(TaskDto input, User user) {

		if (input.id() != null) {
			Optional<Task> optTask = repository.findByIdAndUserId(input.id(), user.getId());
			if (optTask.isEmpty()) {
				throw new TaskNotFoundException();
			}
		}

		Task inputTask = Mapper.toTask(input);
		inputTask.setUser(user);
		if (input.listName() != null) {
			if (input.listName().isBlank()) {
				inputTask.setUserList(null);
			} else {
				UserList userList = userListService.saveByName(input.listName(), user);
				inputTask.setUserList(userList);
			}
		}

		// Find all tasks by name and user
		List<Tag> lstTags = null;
		if (input.tags() != null && !input.tags().isEmpty()) {
			// If missing some, create them
			lstTags = new ArrayList<>();
			for (TagDto tag : input.tags()) {
				lstTags.add(tagService.findOrCreateByUser(user, Mapper.toTag(tag)));
			}

		}
		// Associate final list of tags to task
		inputTask.setTags(lstTags);

		Task task = repository.save(inputTask);
		return Mapper.toTaskDto(task);
	}
	
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
			throw new InvalidDataException("A search term must be provided for the selected filter.");
		}
	}
	
	public TaskDto getById(UUID id, User user) {
		if (id == null || user == null || user.getId() == null)
			throw new TaskNotFoundException();

		Optional<Task> optTask = repository.findByIdAndUserId(id, user.getId());
		if (optTask.isEmpty()) {
			throw new TaskNotFoundException();
		}
		
		return Mapper.toTaskDto(optTask.get());
	}
	
	public void deleteById(UUID id, User user) {
		if (id == null || user == null || user.getId() == null)
			throw new TaskNotFoundException();

		Optional<Task> optTask = repository.findByIdAndUserId(id, user.getId());
		if (optTask.isEmpty()) {
			throw new TaskNotFoundException();
		}
		
		repository.deleteById(id);
	}
	
}
