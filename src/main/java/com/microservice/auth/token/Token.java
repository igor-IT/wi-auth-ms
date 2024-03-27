package com.microservice.auth.token;

import com.microservice.auth.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tokens")
public class Token {

  @Id
  public String id;

  public String token;

  public TokenType tokenType = TokenType.BEARER;

  public boolean revoked;

  public boolean expired;

  @DBRef
  public User user;
}
