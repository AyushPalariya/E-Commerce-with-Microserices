package com.example.PaymentService.Dto;

import com.example.PaymentService.Entities.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusOrder {
    private Long orderId;
    private Status status;
}
