package com.example.ProductService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name="products")

public class Products implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String category;
    private String description;
    private String img;
    private Double amount;
    @Column(unique = true)
    private String name;
    @CreationTimestamp
    private LocalDateTime created;
    @UpdateTimestamp 
    private LocalDateTime updated;

    public Products(String category, String description, String img, Double amount, String name) {
        this.category = category;
        this.description = description;
        this.img = img;
        this.amount = amount;
        this.name = name;
    }
}
