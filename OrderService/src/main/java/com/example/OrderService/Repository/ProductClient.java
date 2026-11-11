package com.example.OrderService.Repository;

import com.example.OrderService.Dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(name = "PRODUCTSERVICE")
public interface ProductClient {
    @PostMapping("/products/bulk")
     ResponseEntity<List<ProductDto>> getProducts(@RequestBody List<Long> ids);
}
