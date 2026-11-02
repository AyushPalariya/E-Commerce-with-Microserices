package com.example.PaymentService.GlobalException;

public class BadRequestExceptionClass extends RuntimeException {
    public BadRequestExceptionClass(String mess){
        super(mess);
    }
}
