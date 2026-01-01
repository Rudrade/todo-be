package me.rudrade.todo.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Task {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;
	
	@Column(name = "title", nullable = false, length = 100)
	@NotBlank(message = "Title must not be blank")
	@Size(max = 100, message = "Title must not exceed 100 characters")
	private String title;
	
	@Column(name = "description", length = 500)
	@Size(max = 500, message = "Description must not exceed 500 characters")
	private String description;
	
	@Column(name = "due_date")
	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, updatable = false)
	@NotNull(message = "An user must be associated")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_list_id")
	private UserList userList;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "tag_task",
		joinColumns = @JoinColumn(name = "task_id"),
		inverseJoinColumns = @JoinColumn(name = "tag_id")
	)
	private List<Tag> tags;


	@Override
	public String toString() {
		return "Task{" +
			"id=" + id +
			", title='" + title + '\'' +
			", description='" + description + '\'' +
			", dueDate=" + dueDate +
			", userList=" + userList +
			", tags=" + tags +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Task task = (Task) o;
		return Objects.equals(id, task.id) && Objects.equals(title, task.title) && Objects.equals(description, task.description) && Objects.equals(dueDate, task.dueDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, title, description, dueDate);
	}
}
