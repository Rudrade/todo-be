package me.rudrade.todo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tag", uniqueConstraints = {@UniqueConstraint(columnNames = {"user", "name"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Name must not be blank")
    private String name;

    @Column(name = "color", nullable = false)
    @NotBlank(message = "Color must not be blank")
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @NotNull(message = "An user must be associated")
    private User user;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "tags")
    private List<Task> tasks;
}
