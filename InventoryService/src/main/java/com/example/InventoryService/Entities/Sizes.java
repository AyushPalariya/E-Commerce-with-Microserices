package com.example.InventoryService.Entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "inventory_size")
public class Sizes implements Serializable {
    @Id
    private Long sizeId;
    private String size;
    private Long totalQuantity;
    private Long availableQuantity;
    private Long reservedQuantity;
    private Long soldQuantity;
    @ManyToOne
    @JoinColumn(name = "color_id",referencedColumnName = "id")
    private Colors colors;
}
