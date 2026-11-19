package com.civicshield.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRegistrationDTO {
    @NotBlank(message = "Full name is required.")
    @Size(min=2, max=100)
    private String fullName;

    @NotBlank(message = "Email address is required.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Please enter a valid 10-digit mobile number.")
    private String mobile;

    @NotBlank(message = "Password is required.")
    @Size(min=8, message = "Password must be at least 8 characters.")
    private String password;

    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
