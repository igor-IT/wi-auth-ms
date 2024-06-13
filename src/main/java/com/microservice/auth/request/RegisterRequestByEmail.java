package com.microservice.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microservice.auth.data.AccountType;
import com.microservice.auth.data.Locale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestByEmail {

    @NotBlank
    @Size(min = 2, message = "First name must not be less than two characters")
    @JsonProperty("first_name")
    private String firstname;
    @NotBlank
    @Size(min = 2, message = "Last name must not be less than two characters")
    @JsonProperty("last_name")
    private String lastname;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$")
    private String email;
    @NotBlank
    @Size(min = 8, message = "Password must be min 8 symbols")
    private String password;
    @JsonProperty("media_photo_id")
    private String mediaPhotoId;
    private Locale locale;
    @NotNull
    @JsonProperty("account_type")
    private AccountType accountType;
}
