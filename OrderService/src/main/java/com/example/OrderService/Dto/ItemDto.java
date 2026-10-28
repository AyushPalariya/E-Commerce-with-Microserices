package com.example.OrderService.Dto;

import com.example.OrderService.Entities.OrderEntity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItemDto {
    private Long id;//include in response
    private Long product_id;
    private String product_name;
    private int quantities;
    private double subAmount;
}
