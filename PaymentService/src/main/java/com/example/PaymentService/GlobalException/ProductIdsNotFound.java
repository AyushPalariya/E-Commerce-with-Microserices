package com.example.PaymentService.GlobalException;

public class ProductIdsNotFound extends RuntimeException {
    public ProductIdsNotFound(String s) {
        super(s);
    }
}
