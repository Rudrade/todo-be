package me.rudrade.todo.config;

import java.util.List;

import me.rudrade.todo.model.types.Role;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	private final AuthenticationProvider authenticationProvider;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	
	@Value("${cors-url}")
	private String[] corsUrl;

    @Value("${profile.active}")
    private String currentProfile;

	public SecurityConfig(AuthenticationProvider authenticationProvider,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.authenticationProvider = authenticationProvider;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	RoleHierarchy roleHierarchy() {
		return RoleHierarchyImpl.withDefaultRolePrefix()
			.role(Role.ROLE_ADMIN.getSuffix()).implies(Role.ROLE_USER.getSuffix())
			.build();
	}
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeHttpRequests -> {
                    authorizeHttpRequests
						.requestMatchers("/health/**").permitAll()
                        .requestMatchers("/todo/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/todo/api/users/register").permitAll()
						.requestMatchers(HttpMethod.PATCH, "/todo/api/users/{id}").hasAuthority(Role.ROLE_USER.name())
						.requestMatchers("/todo/api/users/**").hasAuthority(Role.ROLE_ADMIN.name())
                        .requestMatchers("/todo/api/tag/**").hasAuthority(Role.ROLE_USER.name())
						.requestMatchers("/todo/api/task/**").hasAuthority(Role.ROLE_USER.name());

                    if (!"prod".equals(currentProfile)) {
                        authorizeHttpRequests.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }
                })
                .sessionManagement(sessionManagement ->
                		sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .build();
	}
	
	private CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(corsUrl));
		configuration.setAllowedMethods(List.of("GET", "POST", "DELETE", "PATCH"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
	
}
