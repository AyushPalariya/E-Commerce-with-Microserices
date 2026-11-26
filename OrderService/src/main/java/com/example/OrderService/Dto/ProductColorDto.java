package com.example.OrderService.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductColorDto {
    private Long colorId;
    private String color;
    private Long colorOrderSequence;
    private String images;
    private List<ProductSizeDto> sizes;
}
