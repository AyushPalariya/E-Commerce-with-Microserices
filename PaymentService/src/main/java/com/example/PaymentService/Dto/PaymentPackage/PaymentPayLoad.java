package com.example.PaymentService.Dto.PaymentPackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class PaymentPayLoad {
    public String entity;
    public String account_id;
    public String event;
    public ArrayList<String> contains;
    public Payload payload;
    public Long created_at;
}
