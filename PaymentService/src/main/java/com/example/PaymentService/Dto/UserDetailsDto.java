package com.example.PaymentService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDetailsDto {
    private String email;
    private Long userId;
}
