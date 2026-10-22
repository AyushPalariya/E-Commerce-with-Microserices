package com.example.UserService.GlobalExcep;

public class BadCredentialClass extends RuntimeException{
    public BadCredentialClass(String mess){
        super(mess);
    }
}
