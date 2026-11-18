package com.example.ProductService.Repository;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name="INVENTORYSERVICE")
public interface InventoryFeign {
    @GetMapping("/inventory/maxSold")
    List<Long> getProductIdsWithMaxSold();
}
