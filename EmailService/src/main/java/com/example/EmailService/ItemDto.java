package com.example.EmailService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;//include in response
    private Long product_id;
    private String product_name;
    private int quantities;
    private double subAmount;
}
