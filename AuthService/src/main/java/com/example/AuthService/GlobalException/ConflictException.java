package com.example.AuthService.GlobalException;

public class ConflictException extends RuntimeException{

    public ConflictException(String mess){
        super(mess);
    }
}
