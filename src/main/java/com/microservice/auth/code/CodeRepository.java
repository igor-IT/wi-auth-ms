package com.microservice.auth.code;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CodeRepository extends MongoRepository<Code, String> {

	Optional<Code> findFirstByPhoneOrderByDateDesc(String phone);
}
