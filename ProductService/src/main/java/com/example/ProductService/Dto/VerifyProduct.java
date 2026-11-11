package com.example.ProductService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class VerifyProduct {
    private Long id;
    private String name;

    public VerifyProduct(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
