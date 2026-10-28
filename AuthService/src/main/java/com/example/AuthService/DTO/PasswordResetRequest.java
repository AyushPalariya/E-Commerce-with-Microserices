package com.example.AuthService.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordResetRequest {
    private String email;
    private String newPassword;
    private String confirmPassword;
}
