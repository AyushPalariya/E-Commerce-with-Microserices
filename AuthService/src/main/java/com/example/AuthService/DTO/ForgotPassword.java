package com.example.AuthService.DTO;

import lombok.Data;

import java.time.LocalDateTime;

//for kafka sending data
@Data
public class ForgotPassword {
    private String name;
    private String email;
    private String otp;
}
