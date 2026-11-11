package com.example.ProductService.ExceptionHandle;

public class ProductIdsNotFound extends RuntimeException {
    public ProductIdsNotFound(String s) {
        super(s);
    }
}
