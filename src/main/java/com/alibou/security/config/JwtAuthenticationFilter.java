package com.alibou.security.config;

import com.alibou.security.token.RefreshToken;
import com.alibou.security.token.RefreshTokenRepository;
import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		if (request.getServletPath().contains("/api/v1/auth")) {
			filterChain.doFilter(request, response);
			return;
		}
		if (request.getServletPath().contains("/api/v1/auth")) {
			filterChain.doFilter(request, response);
			return;
		}
		final String authHeader = request.getHeader("X-AUTH-ACCESS-TOKEN");
		final String jwt;
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		jwt = authHeader.substring(7);

		if (!jwtService.isTokenExpired(jwt)) {
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
		} else {
			String refreshToken = request.getHeader("X-AUTH-REFRESH-TOKEN");
			RefreshToken refreshTokenFromDb = refreshTokenRepository.findByToken(refreshToken).orElse(null);
			if (refreshTokenFromDb != null && !jwtService.isTokenExpired(refreshTokenFromDb.getToken())) {
				String userId = jwtService.extractUserId(refreshTokenFromDb.getToken());
				String userPhone = jwtService.extractPhone(refreshTokenFromDb.getToken());
				User userDetails = (User) this.userDetailsService.loadUserByUsername(userPhone);
				String newJwt = jwtService.generateToken(userDetails);
				String newRefreshToken = jwtService.generateRefreshToken(userDetails);
				response.setHeader("X-AUTH-ACCESS-TOKEN", newJwt);
				response.setHeader("X-AUTH-REFRESH-TOKEN", newRefreshToken);
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
}