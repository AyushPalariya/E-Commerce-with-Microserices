package com.example.OrderService.Dto;

import lombok.Data;

@Data
public class ProductDto {
    private Long id;
    private String category;
    private String description;
    private String img;
    private Double amount;
    private String name;
}
