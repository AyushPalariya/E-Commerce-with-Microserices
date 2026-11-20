package com.example.ProductService.Repository;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name="INVENTORYSERVICE")
public interface InventoryFeign {
    @GetMapping("/inventory/internal/maxSold")
    ResponseEntity<List<Long>> getProductIdsWithMaxSold();
    @DeleteMapping("/inventory/internal/deleteAll")
    ResponseEntity<Void> deleteAllInventoryData();
    @DeleteMapping("/inventory/internal/delete")
    ResponseEntity<Void> deleteInventoryDataWithIds(@RequestBody List<Long> uniqueIds);
}
