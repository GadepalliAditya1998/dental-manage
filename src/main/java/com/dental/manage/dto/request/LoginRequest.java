package com.dental.manage.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication request payload carrying base64-encoded username and password
 * to prevent plain-text exposure in network monitoring tools.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Username is required (base64 encoded)")
    private String username;

    @NotBlank(message = "Password is required (base64 encoded)")
    private String password;
}
