package com.microservice.auth.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User implements UserDetails {

	@Id
	private String id;
	private String firstname;
	private String lastname;
	private String email;
	private String phone;
	private String password;
	private Locale locale;
	private AccountType accountType;
	private boolean isEnabled;
	private String imageId;
	@JsonProperty("timestamp")
	private Date registrationDate;

	private boolean isPhoneVerified;
	private boolean isEmailVerified;

	@DBRef
	private Set<Role> roles;

	@DBRef
	private RefreshToken token;

	public User addRole(Role role) {
		this.roles.add(role);
		return this;
	}


	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roles.stream()
				.flatMap(role -> role.getAuthorities().stream())
				.collect(Collectors.toList());
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return id;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
