package com.alibou.security.auth;

import com.alibou.security.user.Locale;
import com.alibou.security.user.Role;
import jakarta.validation.constraints.*;
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
  private String first_name;
  @NotBlank
  @Size(min = 2, message= "Last name must not be less than two characters")
  private String last_name;
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
  private String media_photo_id;
  private Locale locale;
}
