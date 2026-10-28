package com.example.ProductService.ExceptionHandle;

public class BadRequestException extends RuntimeException{
    public BadRequestException(String s){
        super(s);
    }
}
