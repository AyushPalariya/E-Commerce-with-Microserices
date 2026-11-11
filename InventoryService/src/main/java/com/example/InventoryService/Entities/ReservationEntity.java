package com.example.InventoryService.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "reserved")
public class ReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long productId;
    private Long quantities;
}
