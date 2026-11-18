package com.example.OrderService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "order_item")
@Data
@NoArgsConstructor
public class OrderItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long product_id;
    private String product_name;
    private Long quantities;
    private Double subAmount;
    private Double perAmount;
    private String color;
    private String size;
    @ManyToOne
    @JoinColumn(name = "order_id",referencedColumnName = "id")
    private OrderEntity order;


}
