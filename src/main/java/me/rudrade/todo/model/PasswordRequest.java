package me.rudrade.todo.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
@Table(name="password_request")
public class PasswordRequest {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true, updatable = false)
    @NotNull
    private UUID id;

    @NotNull
    @ManyToOne(optional=false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotNull
    @Column(name = "dt_created", nullable = false, updatable = false)
    private LocalDateTime dtCreated;

    @Column(name = "mail_sent")
    private boolean mailSent;

}
