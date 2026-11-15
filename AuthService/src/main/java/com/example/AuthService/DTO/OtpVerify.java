package com.example.AuthService.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OtpVerify {
    private String email;
    private String otp;
}
