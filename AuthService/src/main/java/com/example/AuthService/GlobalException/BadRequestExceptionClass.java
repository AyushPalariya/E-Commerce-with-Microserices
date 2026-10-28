package com.example.AuthService.GlobalException;

public class BadRequestExceptionClass extends RuntimeException {
    public BadRequestExceptionClass(String s) {
        super(s);
    }
}
