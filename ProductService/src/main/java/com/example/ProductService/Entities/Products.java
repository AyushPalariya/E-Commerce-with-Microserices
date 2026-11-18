package com.example.ProductService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

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
    private String name;
    @ElementCollection
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "color", nullable = false)
    @OrderColumn(name = "color_order")
    private List<String> color;
    @ElementCollection
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size", nullable = false)
    @OrderColumn(name = "size_order")
    private List<String> size;
    private Double avgRating;
    @Enumerated(value = EnumType.STRING)
    private Gender gender;
    private Long discountPercentage;
    private Double discountAmount;
    @CreationTimestamp
    private LocalDateTime created;
    @UpdateTimestamp 
    private LocalDateTime updated;

}
