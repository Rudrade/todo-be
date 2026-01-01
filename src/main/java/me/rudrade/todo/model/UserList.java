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

@Table(name = "user_list")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Name must not be blank")
    private String name;

    @Column(name = "color", nullable = false)
    @NotNull(message = "Color must exist")
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @NotNull(message = "An user must be asssociated")
    private User user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "userList")
    private List<Task> tasks;
}
