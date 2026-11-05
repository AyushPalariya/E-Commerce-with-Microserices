package com.example.EmailService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlace implements Serializable {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private String name;
    private Double totalAmount;
    private String status;
    private LocalDateTime datetime;
    private List<ItemDto> items;
}
