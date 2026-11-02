package com.example.PaymentService.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentOrderDto {
    private Double totalAmount;
    private String status;
}
