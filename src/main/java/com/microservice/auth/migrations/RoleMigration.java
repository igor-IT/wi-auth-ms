package com.microservice.auth.migrations;

import com.microservice.auth.user.Role;
import com.microservice.auth.user.RoleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class RoleMigration implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        Role role = new Role(RoleStatus.USER, Collections.emptySet());
        Query query = new Query();
        Criteria id = Criteria.where("name").is(role.getName());
        query.addCriteria(id);
        Role roleFromDb = mongoTemplate.findOne(query, Role.class);
        if (roleFromDb == null) {
            mongoTemplate.save(role);
        }
    }
}
