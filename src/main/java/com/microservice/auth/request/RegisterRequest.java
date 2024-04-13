package com.microservice.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microservice.auth.data.AccountType;
import com.microservice.auth.data.Locale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Size(min = 2, message = "First name must not be less than two characters")
    @JsonProperty("first_name")
    private String firstname;
    @NotBlank
    @Size(min = 2, message = "Last name must not be less than two characters")
    @JsonProperty("last_name")
    private String lastname;
    @NotBlank
    private String email;
    @NotBlank
    private String phone;
    @NotBlank
    @Size(min = 8, message = "Password must be min 8 symbols")
    private String password;
    @JsonProperty("media_photo_id")
    private String mediaPhotoId;
    private Locale locale;
    @NotNull
    private AccountType accountType;
}
