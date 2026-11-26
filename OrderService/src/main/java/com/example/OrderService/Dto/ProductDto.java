package com.example.OrderService.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductDto {
    private Long id;
    private String category;
    private String description;
    private String name;
    private List<ProductColorDto> colors;
    private Double avgRating;
    private String gender;
    private Long discountPercentage;
    private Boolean onSale;
}
