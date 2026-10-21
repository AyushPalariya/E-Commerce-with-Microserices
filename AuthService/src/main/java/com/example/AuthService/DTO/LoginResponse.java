package com.example.AuthService.DTO;

import com.example.AuthService.Entities.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private Role role;

    public LoginResponse(String accessToken, String refreshToken, String email, Role role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.role = role;
    }
}
