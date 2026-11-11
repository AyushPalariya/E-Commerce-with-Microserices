package com.example.InventoryService.Global;

public class BadRequestExceptionClass extends RuntimeException{
    public BadRequestExceptionClass(String mess){
        super(mess);
    }
}
