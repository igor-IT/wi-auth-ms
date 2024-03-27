package com.microservice.auth.auth;

import com.microservice.auth.code.Type;
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
	@Pattern(regexp = "^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$\n")
	private String phone;
	@NotBlank
	// TOOD : add validation
	private Type type;

	@Pattern(regexp = "/\\d{4}/g")
	private String code;
}
