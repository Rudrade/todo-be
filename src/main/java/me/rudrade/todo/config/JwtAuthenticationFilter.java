package me.rudrade.todo.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
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
import me.rudrade.todo.exception.InvalidAccessException;
import me.rudrade.todo.service.JwtService;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	
	@Autowired private JwtService jwtService;
	@Autowired private UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		final String authHeader = request.getHeader("Authorization");
		
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		try {
			final String jwt = authHeader.substring(7);
			final String username = jwtService.extractUsername(jwt);
			
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (username != null && authentication == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				
				if (jwtService.isTokenValid(jwt, username)) {
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
			
			throw new InvalidAccessException();
			
		} catch (Exception e) {
			throw new InvalidAccessException();
		}
		
	}
	
}
