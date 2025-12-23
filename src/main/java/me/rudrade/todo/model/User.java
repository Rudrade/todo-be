package me.rudrade.todo.model;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.Setter;

@Table(name = "USER")
@Entity
@Getter
@Setter
public class User implements UserDetails {
	
	@Serial
    private static final long serialVersionUID = -4583113882616923801L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(nullable = false, unique = true)
	private String username;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
	private List<UserList> userLists;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
	private List<Tag> tags;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of();
	}

	@Override
	public String getPassword() {
		return null;
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
