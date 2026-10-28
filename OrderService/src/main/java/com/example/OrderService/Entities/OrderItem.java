package com.example.OrderService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item")
@Data
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long product_id;
    private String product_name;
    private int quantities;
    private double subAmount;
    @ManyToOne
    @JoinColumn(name = "order_id",referencedColumnName = "id")
    private OrderEntity order;


}
