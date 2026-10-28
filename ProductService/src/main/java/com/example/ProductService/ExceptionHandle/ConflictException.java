package com.example.ProductService.ExceptionHandle;

public class ConflictException extends RuntimeException {
    public ConflictException(String s) {
        super(s);
    }
}
