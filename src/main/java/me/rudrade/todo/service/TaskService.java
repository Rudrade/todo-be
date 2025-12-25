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
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import me.rudrade.todo.dto.Mapper;
import me.rudrade.todo.dto.TaskDto;
import me.rudrade.todo.exception.TaskNotFoundException;
import me.rudrade.todo.model.Task;
import me.rudrade.todo.repository.TaskRepository;
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

	public TaskDto saveTask(@Nonnull TaskDto input) {
		return saveTask(input, null);
	}

	public TaskDto saveTask(@Nonnull TaskDto input, User user) {

		if (input.id() != null) {
			Optional<Task> optTask = repository.findById(input.id());
			if (optTask.isEmpty()) {
				throw new TaskNotFoundException();
			}
		}

		Task inputTask = Mapper.toTask(input);
		if (input.listName() != null) {
			if (input.listName().isEmpty()) {
				inputTask.setUserList(null);
			} else {
				UserList userList = userListService.saveByName(input.listName(), user);
				inputTask.setUserList(userList);
			}
		}

		// Find all tasks by name and user
		List<Tag> lstTags = null;
		if (input.tags() != null) {
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
	
	public TaskListResponse getAll(@Nonnull TaskListFilter filter) {
        Iterable<Task> result;
        long count;
        if (Filter.TODAY.equals(filter.filter())) {
            result = repository.findDueToday();
            count = repository.countFindDueToday();

        } else if (Filter.UPCOMING.equals(filter.filter())) {
            result = repository.findDueUpcoming();
            count = repository.countFindDueUpcoming();

        } else if (Filter.SEARCH.equals(filter.filter()) && filter.searchTerm() != null) {
			result = repository.findByTitleContains(filter.searchTerm());
			count = repository.countByTitleContains(filter.searchTerm());

		} else if (Filter.LIST.equals(filter.filter()) && filter.searchTerm() != null) {
			Optional<UserList> lst = userListService.findByName(filter.searchTerm());
			if (lst.isPresent()) {
				result = lst.get().getTasks();
				count = lst.get().getTasks().size();
			} else {
				result = List.of();
				count = 0;
			}

		} else if (Filter.TAG.equals(filter.filter()) && filter.searchTerm() != null) {
			Optional<Tag> tag = tagService.findByName(filter.searchTerm());
			if (tag.isPresent()) {
				result = tag.get().getTasks();
				count = tag.get().getTasks().size();
			} else {
				result = List.of();
				count = 0;
			}

        } else {
            result = repository.findAll();
            count = repository.count();
        }

        List<TaskDto> lst = new ArrayList<>();
        result.forEach(t -> lst.add(Mapper.toTaskDto(t)));
		
		return new TaskListResponse(count, lst);
	}
	
	public TaskDto getById(@Nonnull UUID id) {
		Optional<Task> optTask = repository.findById(id);
		if (optTask.isEmpty()) {
			throw new TaskNotFoundException();
		}
		
		return Mapper.toTaskDto(optTask.get());
	}
	
	public void deleteById(@Nonnull UUID id) {
		Optional<Task> optTask = repository.findById(id);
		if (optTask.isEmpty()) {
			throw new TaskNotFoundException();
		}
		
		repository.deleteById(id);
	}
	
}
