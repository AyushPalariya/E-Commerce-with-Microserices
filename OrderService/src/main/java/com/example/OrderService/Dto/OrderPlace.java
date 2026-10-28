package com.example.OrderService.Dto;

import com.example.OrderService.Entities.OrderItem;
import com.example.OrderService.Entities.Status;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlace {
    private Long orderId;//include in response
    private String userId;
    private String userEmail;
    private String name;
    private Double totalAmount;
    private String status;//include in response
    private LocalDateTime datetime;//include in response
    private List<ItemDto> items;
}
