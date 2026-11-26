package com.example.ProductService.Entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
@Data
@Entity
@Table(name = "products_size")
public class Sizes implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String size;
    private Double amount;
    @ManyToOne
    @JoinColumn(name = "color_id",referencedColumnName = "id")
    private Colors colors;
}
