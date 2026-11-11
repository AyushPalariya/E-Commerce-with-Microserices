package com.example.InventoryService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Inventory implements Serializable {
    //include all in response when getAllStock of productId
    private Long id;//include in response
    //include in request when adding upto totalQuantity
    //include in request when getStock upto productId
    private String name;
    private Long productId;
    private Long totalQuantity;

    private Long availableQuantity;
    private Long reservedQuantity;
    private Long soldQuantity;

    //adding response
    public Inventory(Long id, String name, Long productId, Long totalQuantity) {
        this.id = id;
        this.name = name;
        this.productId = productId;
        this.totalQuantity = totalQuantity;
    }
}
