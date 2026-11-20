package com.example.InventoryService.Repository;

import com.example.InventoryService.Dto.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "PRODUCTSERVICE", contextId = "inventoryProductClient")
public interface ProductClientInterface {

    @PostMapping("/products/verify")
    ResponseEntity<List<Product>> verifyProductIds(@RequestBody List<Product> productList);
}
