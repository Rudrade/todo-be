package me.rudrade.todo.model;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import me.rudrade.todo.model.types.Language;
import me.rudrade.todo.model.types.Role;

@Table(name = "user_request")
@Entity
@Getter
@Setter
public class UserRequest {

    @Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;
	
	@Column(name = "username", nullable = false, unique = true)
	@NotBlank(message = "Username must not be blank")
	private String username;

	@Column(name = "password", nullable = false)
	@NotBlank(message = "Password must not be blank")
	private String password;

	@Column(name = "email", nullable = false, unique = true)
	@Email(message = "Email must be a valid address")
	@NotBlank(message = "Email must not be blank")
	private String email;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	private Role role;

    @Column(name = "dt_created", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime dtCreated;

	@Column(name = "mail_sent", nullable = false)
	private boolean mailSent;

	@Column(name = "language", nullable = false)
	@Enumerated(EnumType.STRING)
	private Language language = Language.EN;

	public Locale getLocale() {
		if (language == null) return Locale.ENGLISH;

		return Locale.of(language.name());
	}
}
