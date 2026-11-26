package com.example.ProductService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
    private String name;
    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Colors> colors;
    private Double avgRating;
    @Enumerated(value = EnumType.STRING)
    private Gender gender;
    private Long discountPercentage;
    @CreationTimestamp
    private LocalDateTime created;
    @UpdateTimestamp 
    private LocalDateTime updated;
    private Boolean onSale;

}
