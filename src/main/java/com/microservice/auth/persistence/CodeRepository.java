package com.microservice.auth.persistence;

import com.microservice.auth.data.Code;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CodeRepository extends MongoRepository<Code, String> {

	Optional<Code> findFirstByClientOrderByDateDesc(String phone);
}
