package com.example.AuthService.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class OtpToken implements Serializable {
    private String datetime;
    private String email;
    private String otp;
}
