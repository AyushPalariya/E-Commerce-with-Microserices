package com.example.PaymentService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentEmail {
    private String orderId;
    private String paymentId;
    private OrderPlace order;
}
