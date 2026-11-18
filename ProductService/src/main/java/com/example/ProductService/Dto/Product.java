package com.example.ProductService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

import com.example.ProductService.Entities.Gender;

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
    private List<String> color;
    private List<String> size;
    private Double avgRating;
    private Gender gender;
    private Long discountPercentage;
    private Double discountAmount;
}
