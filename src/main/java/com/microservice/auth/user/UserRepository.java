package com.microservice.auth.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

  Optional<User> findByPhone(String phone);
  User findByEmail(String email);
  boolean existsByPhone(String phone);

}
