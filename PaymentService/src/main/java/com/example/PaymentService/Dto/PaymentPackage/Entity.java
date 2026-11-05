package com.example.PaymentService.Dto.PaymentPackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
@NoArgsConstructor
@Data
@AllArgsConstructor
public class Entity{
    public String id;
    public String entity;
    public Double amount;
    public String currency;
    public String status;
    public String order_id;
    public Object invoice_id;
    public Boolean international;
    public String method;
    public Double amount_refunded;
    public Object refund_status;
    public Boolean captured;
    public Object description;
    public String card_id;
    public Card card;
    public Object bank;
    public Object wallet;
    public Object vpa;
    public String email;
    public String contact;
    public ArrayList<Object> notes;
    public Integer fee;
    public Integer tax;
    public Object error_code;
    public Object error_description;
    public Long created_at;
    public Double amount_paid;
    public Double amount_due;
    public String receipt;
    public Object offer_id;
    public Integer attempts;
}
