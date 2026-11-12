package com.example.InventoryService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "reserved")
@Data
@NoArgsConstructor
public class ReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long productId;
    private Long quantities;
    @Enumerated(EnumType.STRING)
    private Status status;

    public ReservationEntity(Long orderId, Long productId, Long quantities,Status status) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantities = quantities;
        this.status=status;
    }
}
