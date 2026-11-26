package com.example.ProductService.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class SizeDto implements Serializable {
    private Long sizeId;//include in response
    private String size;
    private Double amount;
}
