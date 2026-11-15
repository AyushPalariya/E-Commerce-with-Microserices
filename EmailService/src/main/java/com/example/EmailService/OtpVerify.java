package com.example.EmailService;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OtpVerify {
    private String email;
    private String otp;
}
