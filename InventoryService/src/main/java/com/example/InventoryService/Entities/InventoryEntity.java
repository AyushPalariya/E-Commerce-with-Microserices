package com.example.InventoryService.Entities;

import com.example.InventoryService.Dto.ColorDto;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
public class InventoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private Long productId;
    private String name;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @OneToMany(mappedBy = "inventory",cascade = CascadeType.ALL)
    private List<Colors> colors;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
