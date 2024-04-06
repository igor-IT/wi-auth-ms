package com.microservice.auth.config;

import com.microservice.auth.auth.CustomAuthenticationSuccessHandler;
import com.microservice.auth.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {
	private static final String[] WHITE_LIST_URL = {"/api/v1/auth/**",
			"/v2/api-docs",
			"/v3/api-docs",
			"/v3/api-docs/**",
			"/swagger-resources",
			"/swagger-resources/**",
			"/configuration/ui",
			"/configuration/security",
			"/swagger-ui/**",
			"/webjars/**",
			"/swagger-ui.html",
			"/actuator/**",
			"/actuator/metrics/**"};
	private final JwtAuthenticationFilter jwtAuthFilter;
	private final AuthenticationProvider authenticationProvider;
	private final LogoutHandler logoutHandler;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(req ->
						req.requestMatchers(WHITE_LIST_URL)
								.permitAll()
								.requestMatchers(GET, "/api/v1/management/**").hasAnyAuthority(Permission.ADMIN_READ.name(), Permission.MANAGER_READ.name())
								.requestMatchers(POST, "/api/v1/management/**").hasAnyAuthority(Permission.ADMIN_CREATE.name(), Permission.MANAGER_CREATE.name())
								.requestMatchers(PUT, "/api/v1/management/**").hasAnyAuthority(Permission.ADMIN_UPDATE.name(), Permission.MANAGER_UPDATE.name())
								.requestMatchers(DELETE, "/api/v1/management/**").hasAnyAuthority(Permission.ADMIN_DELETE.name(), Permission.MANAGER_DELETE.name())
								.anyRequest()
								.authenticated()
				)
				.authenticationProvider(authenticationProvider)
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
				.logout(logout ->
						logout.logoutUrl("/api/v1/auth/logout")
								.addLogoutHandler(logoutHandler)
								.logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
				)
				.oauth2Login(config -> config
						.userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
						.successHandler(savedRequestAwareAuthenticationSuccessHandler())
						.defaultSuccessUrl("/"))
		;
		return http.build();
	}

	@Bean
	public SavedRequestAwareAuthenticationSuccessHandler savedRequestAwareAuthenticationSuccessHandler() {
		return new CustomAuthenticationSuccessHandler();
	}

	private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
		return userRequest -> {
			String email = userRequest.getIdToken().getEmail();

			Optional<User> existingUser = userRepository.findByEmail(email);
			UserDetails userDetails = existingUser.orElseGet(() -> {
				Role role = roleRepository.findByName(RoleStatus.USER.name()).orElseThrow();
				User newUser = User.builder()
						.firstname(userRequest.getIdToken().getGivenName())
						.lastname(userRequest.getIdToken().getFamilyName())
						.email(email)
						.locale(Locale.EN)
						.roles(new HashSet<>(List.of(role)))
						.accountType(AccountType.USUAL)
						.build();
				return userRepository.save(newUser);
			});

			DefaultOidcUser oidcUser = new DefaultOidcUser(userDetails.getAuthorities(), userRequest.getIdToken());

			Set<Method> userDetailsMethods = Set.of(UserDetails.class.getMethods());
			return (OidcUser) Proxy.newProxyInstance(
					SecurityConfiguration.class.getClassLoader(),
					new Class[]{UserDetails.class, OidcUser.class},
					(proxy, method, args) -> userDetailsMethods.contains(method)
							? method.invoke(userDetails, args)
							: method.invoke(oidcUser, args)
			);
		};
	}
}
