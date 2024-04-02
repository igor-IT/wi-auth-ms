package com.microservice.auth.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microservice.auth.user.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

  @NotBlank
  @Size(min = 2, message= "First name must not be less than two characters")
  @JsonProperty("first_name")
  private String firstname;
  @NotBlank
  @Size(min = 2, message= "Last name must not be less than two characters")
  @JsonProperty("last_name")
  private String lastname;
  @NotBlank
  @Email
  private String email;
  @NotBlank
  @Pattern(regexp = "^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$")
  private String phone;
  private String code;
  @NotBlank
  @Size(min = 8, message = "Password must be min 8 symbols")
//  @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*\\W)(?!.* )",
//          message = "Password must contain one digit, one lowercase letter, one uppercase letter")
  private String password;
  @JsonProperty("media_photo_id")
  private String mediaPhotoId;
  private Locale locale;
}
