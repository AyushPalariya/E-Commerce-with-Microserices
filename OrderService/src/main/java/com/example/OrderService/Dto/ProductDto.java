package com.example.OrderService.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductDto {
    private Long id;
    private String category;
    private String description;
    private String img;
    private Double amount;
    private String name;
    private List<String> color;
    private List<String> size;
    private Double avgRating;
    private String gender;
    private Long discountPercentage;
    private Double discountAmount;
}
