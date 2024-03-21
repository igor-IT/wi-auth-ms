package com.alibou.security.auth;

import com.alibou.security.user.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.intellij.lang.annotations.RegExp;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SMSCodeRequest {
	@NotBlank
	@Pattern(regexp = "^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$\n")
	private String phone;
	private Locale locale;
}
