package com.microservice.auth.token;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
public interface TokenRepository extends MongoRepository<Token, String> {

  List<Token> findAllByUserIdAndExpiredFalseAndRevokedFalse(String user_id);

  Optional<Token> findByToken(String token);
}
