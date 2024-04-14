package com.microservice.auth.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Document(collection = "roles")
public class Role {

	@Id
	private String id;
	private Set<Permission> permissions;

	public Role(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	public List<SimpleGrantedAuthority> getAuthorities() {
		return getPermissions()
				.stream()
				.map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
				.collect(Collectors.toList());
	}
}
