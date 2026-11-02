package com.example.OrderService.Dto;

import com.example.OrderService.Entities.OrderEntity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;//include in response
    private Long product_id;
    private String product_name;//include in response
    private int quantities;
    private Double subAmount;//include in response
}
