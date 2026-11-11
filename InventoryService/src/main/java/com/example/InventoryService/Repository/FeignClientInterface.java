package com.example.InventoryService.Repository;

import com.example.InventoryService.Dto.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name="PRODUCTSERVICE")
public interface FeignClientInterface {

    @PostMapping("/products/verify")
    List<Product> verifyProductIds(@RequestBody List<Product> productList);
}
