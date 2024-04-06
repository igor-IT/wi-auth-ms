package com.microservice.auth.auth;

import com.microservice.auth.code.Type;
import com.microservice.auth.user.Locale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidateRequest {
    @NotBlank
    private String phone;
    private Type type;
    @NotBlank
    @Pattern(regexp = "\\d{4}")
    private String code;
    private Locale locale;
}
