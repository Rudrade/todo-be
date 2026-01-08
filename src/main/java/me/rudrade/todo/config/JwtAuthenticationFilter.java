package me.rudrade.todo.config;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.repository.UserRepository;
import me.rudrade.todo.service.JwtService;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		final var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		final var jwt = authHeader.substring(7);
		final var subject = jwtService.getSubjectId(jwt);
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (subject != null && authentication == null &&
			jwtService.isTokenValidWithLeeway(jwt, subject)
		) {
			var user = userRepository.findById(subject).orElseThrow(InvalidAccessException::new);
			if (user.isActive()) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
				if (userDetails != null) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							userDetails,
							null,
							userDetails.getAuthorities());
					
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
					
					filterChain.doFilter(request, response);
					return;
				}
			}
		}
		
		throw new InvalidAccessException();
	}
	
}
