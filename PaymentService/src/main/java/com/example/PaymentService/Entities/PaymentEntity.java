package com.example.PaymentService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "payment")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private Long orderId;
    @Column(unique = true)
    private String raz_order_id;
    @Column(unique = true)
    private String raz_payment_id;//null
    private Double amount;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String email;
    private Long user_id;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;



    public PaymentEntity(Long order_id, String raz_order_id, Double amount, Status status, String email, Long user_id) {
        this.orderId = order_id;
        this.raz_order_id = raz_order_id;
        this.amount = amount;
        this.status = status;
        this.email = email;
        this.user_id = user_id;
    }
}
