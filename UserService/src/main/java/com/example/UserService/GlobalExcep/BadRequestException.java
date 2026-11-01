package com.example.UserService.GlobalExcep;

public class BadRequestException extends RuntimeException{
    public BadRequestException(String mess){
        super(mess);
    }
}
