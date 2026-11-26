package com.example.EmailService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;
    private Long product_id;
    private String product_name;
    private Long quantities;
    private double subAmount;
    private double perAmount;
    private String color;
    private String size;
}
