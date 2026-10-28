package com.example.ProductService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product implements Serializable {
    private Long id;//include in response
    private String category;
    private String description;
    private String img;
    private Double amount;
    private String name;


}
