package com.example.InventoryService.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class ColorDto implements Serializable {
    private Long colorId;
    private String color;
    private String images;
    private List<SizeDto> sizes;
}
