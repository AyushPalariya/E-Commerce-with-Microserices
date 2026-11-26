package com.example.InventoryService.Dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

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
    private String gender;
    private List<ColorDto> colors;

}
