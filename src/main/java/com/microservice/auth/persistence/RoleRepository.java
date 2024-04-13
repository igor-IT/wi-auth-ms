
package com.microservice.auth.persistence;

import com.microservice.auth.data.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, String> {
	Optional<Role> findByName(String name);
}
