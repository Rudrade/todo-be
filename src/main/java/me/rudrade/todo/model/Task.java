package me.rudrade.todo.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
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
	@Column(name = "id", updatable = false)
	private UUID id;
	
	@Column(name = "title", nullable = false, length = 100)
	private String title;
	
	@Column(name = "description", length = 500)
	private String description;
	
	@Column(name = "due_date")
	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_list_id")
	private UserList userList;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "TAG_TASK",
		joinColumns = @JoinColumn(name = "TAG_ID"),
		inverseJoinColumns = @JoinColumn(name = "TASK_ID")
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
