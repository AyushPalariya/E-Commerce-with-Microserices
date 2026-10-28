package com.example.EmailService;

import lombok.Data;

@Data
public class ForgotPassword {
    private String name;
    private String email;
    private String otp;

}
