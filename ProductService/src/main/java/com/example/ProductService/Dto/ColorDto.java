package com.example.ProductService.Dto;

import com.example.ProductService.Entities.Sizes;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
@Data
@NoArgsConstructor
public class ColorDto implements Serializable {
    private Long colorId;//include in response
    private String color;
    private Long colorOrderSequence;
    private String images;
    private List<SizeDto> sizes;

}
