package com.example.InventoryService.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Entity
@Table(name="inventory_color")
@Data
@NoArgsConstructor
public class Colors implements Serializable {
    @Id
    private Long id;
    private String color;
    private String images;
    @OneToMany(mappedBy = "colors", cascade = CascadeType.ALL)
    private List<Sizes> sizes;
    @ManyToOne
    @JoinColumn(name = "inventory_id",referencedColumnName = "id")
    private InventoryEntity inventory;
}
