package com.example.ProductService.Dto;

import com.example.ProductService.Entities.Colors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.ProductService.Entities.Gender;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product implements Serializable {
    private Long id;//include in response not in request
    private String category;
    private String description;
    private String name;
    private List<ColorDto> colors;
    private Double avgRating;
    private String gender;
    private Long discountPercentage;
    private Boolean onSale;//not include in request
}
