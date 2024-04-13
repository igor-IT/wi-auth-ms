package com.microservice.auth.config;

import com.microservice.auth.data.RefreshToken;
import com.microservice.auth.persistence.RefreshTokenRepository;
import com.microservice.auth.data.User;
import com.microservice.auth.persistence.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	@Value("${application.security.jwt.access-token-header}")
	private String accessTokenHeader;

	@Value("${application.security.jwt.refresh-token-header}")
	private String refreshTokenHeader;

	@Value("${application.security.jwt.auth-base-url}")
	private String authBaseUrl;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
			if (request.getServletPath().contains(authBaseUrl)) {
			filterChain.doFilter(request, response);
			return;
		}
		if (request.getServletPath().contains(authBaseUrl)) {
			filterChain.doFilter(request, response);
			return;
		}
		final String authHeader = request.getHeader(accessTokenHeader);
		final String jwt;
		String bearer = "Bearer ";
		if (authHeader == null || !authHeader.startsWith(bearer)) {
			filterChain.doFilter(request, response);
			return;
		}
		jwt = authHeader.substring(7);

		if (!jwtService.isTokenExpired(jwt)) {
			authorizationUser(request, response, filterChain, jwt);
		} else {
			tryGenerateToken(request, response, filterChain);
		}

	}

	private void authorizationUser(HttpServletRequest request, HttpServletResponse response,
								   FilterChain filterChain, String jwt) throws IOException, ServletException {
		String userPhone = jwtService.extractPhone(jwt);
		UserDetails userDetails = this.userDetailsService.loadUserByUsername(userPhone);
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities()
		);
		authToken.setDetails(
				new WebAuthenticationDetailsSource().buildDetails(request)
		);
		SecurityContextHolder.getContext().setAuthentication(authToken);
		filterChain.doFilter(request, response);
	}

	private void tryGenerateToken(HttpServletRequest request, HttpServletResponse response,
								  FilterChain filterChain) throws IOException, ServletException {
		String refreshToken = request.getHeader(refreshTokenHeader);
		RefreshToken refreshTokenFromDb = refreshTokenRepository.findByToken(refreshToken).orElse(null);
		if (refreshTokenFromDb != null && !jwtService.isTokenExpired(refreshTokenFromDb.getToken())) {
			String userId = jwtService.extractUserId(refreshTokenFromDb.getToken());
			User userDetails = userRepository.findById(userId)
					.orElseThrow(() -> new UsernameNotFoundException("User not found"));
			String newJwt = jwtService.generateToken(userDetails);
			String newRefreshToken = jwtService.generateRefreshToken(userDetails);
			response.setHeader(accessTokenHeader, newJwt);
			response.setHeader(refreshTokenHeader, newRefreshToken);
			refreshTokenRepository.delete(refreshTokenFromDb);
			refreshTokenRepository.save(
					RefreshToken.builder()
							.userId(userId)
							.token(newRefreshToken)
							.expiryDate(Instant.now().plusMillis(jwtService.getRefreshExpiration()))
							.build());

			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
					userDetails,
					null,
					userDetails.getAuthorities()
			);
			authToken.setDetails(
					new WebAuthenticationDetailsSource().buildDetails(request)
			);
			SecurityContextHolder.getContext().setAuthentication(authToken);
		}
		filterChain.doFilter(request, response);
	}
}