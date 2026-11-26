package com.example.InventoryService.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class SizeDto implements Serializable {
    private Long sizeId;
    private String size;
    private Long totalQuantity;
    private Long availableQuantity;
    private Long reservedQuantity;
    private Long soldQuantity;
}
