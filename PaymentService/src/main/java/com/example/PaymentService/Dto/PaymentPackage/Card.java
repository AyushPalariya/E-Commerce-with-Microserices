package com.example.PaymentService.Dto.PaymentPackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class Card{
    public String id;
    public String entity;
    public String name;
    public String last4;
    public String network;
    public String type;
    public Object issuer;
    public Boolean international;
    public Boolean emi;
}
