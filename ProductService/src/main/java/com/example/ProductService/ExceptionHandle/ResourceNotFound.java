package com.example.ProductService.ExceptionHandle;

public class ResourceNotFound extends RuntimeException{
    public ResourceNotFound(String mess){
        super(mess);
    }
}
