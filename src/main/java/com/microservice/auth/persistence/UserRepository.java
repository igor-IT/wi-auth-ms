package com.microservice.auth.persistence;

import com.microservice.auth.data.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

  Optional<User> findByPhone(String phone);
  Optional<User> findByEmail(String email);
  boolean existsByPhone(String phone);

}
