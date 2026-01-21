package me.rudrade.todo.model;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.Setter;
import me.rudrade.todo.model.types.Language;
import me.rudrade.todo.model.types.Role;

@Table(name = "user")
@Entity
@Getter
@Setter
public class User implements UserDetails {
	
	@Serial
    private static final long serialVersionUID = -4583113882616923801L;

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

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@Column(name = "image_version")
	private String imageVersion;

	@Column(name = "language", nullable = false)
	@Enumerated(EnumType.STRING)
	private Language language;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<Task> tasks;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<UserList> userLists;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<Tag> tags;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<PasswordRequest> passwordRequests;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if (this.role == null)
			return List.of();
		return List.of(new SimpleGrantedAuthority(this.role.name()));
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	public Locale getLocale() {
		if (language == null) return Locale.ENGLISH;

		return Locale.of(language.name());
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return Objects.equals(id, user.id) && Objects.equals(username, user.username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, username);
	}

}


