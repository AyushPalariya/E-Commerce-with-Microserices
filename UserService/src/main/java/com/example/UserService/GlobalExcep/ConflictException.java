package com.example.UserService.GlobalExcep;

public class ConflictException extends RuntimeException {
    public ConflictException(String s) {
        super(s);
    }
}
