package com.example.ProductService.Controller;

import com.example.ProductService.Dto.Product;
import com.example.ProductService.Service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/home")
public class HomeController {
    @Autowired
    private HomeService homeService;
    //user
    @GetMapping("/recent")
    public ResponseEntity<Page<Product>> newProducts(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "14") int size){
        return ResponseEntity.ok(homeService.newProducts(page,size));
    }
    //user
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategory(){
        return ResponseEntity.ok(homeService.getCategories());
    }
    @GetMapping("/trending")
    public ResponseEntity<Page<Product>> getTrendingProducts(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "14") int size){
        return ResponseEntity.ok(homeService.trending(page,size));
    }

}
