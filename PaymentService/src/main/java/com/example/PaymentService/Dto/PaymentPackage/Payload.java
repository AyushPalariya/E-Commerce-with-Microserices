package com.example.PaymentService.Dto.PaymentPackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class Payload{
        public Payment payment;
        public Order order;
}

