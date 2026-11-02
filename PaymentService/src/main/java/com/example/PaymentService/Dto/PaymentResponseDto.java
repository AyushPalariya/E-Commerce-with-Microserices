package com.example.PaymentService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponseDto {
    private String raz_order_id;
    private Double totalAmount;
    private String keyId;
}
