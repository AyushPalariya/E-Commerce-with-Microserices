package com.example.ProductService.Entities;

import com.example.ProductService.Dto.Product;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="products_color")
@Data
@NoArgsConstructor
public class Colors implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String color;
    private Long colorOrderSequence;
    private String images;
    @OneToMany(mappedBy = "colors", cascade = CascadeType.ALL)
    private List<Sizes> sizes;
    @ManyToOne
    @JoinColumn(name = "product_id",referencedColumnName = "id")
    private Products products;
}
