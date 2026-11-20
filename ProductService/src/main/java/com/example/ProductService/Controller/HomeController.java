package com.example.ProductService.Controller;

import com.example.ProductService.Dto.Product;
import com.example.ProductService.Service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/home")
@Tag(name = "Home APIs",description = "RestApis for managing Home")
public class HomeController {
    @Autowired
    private HomeService homeService;
    //user
    @Operation(summary = "Get new Products that added recently",description = "Take requestParam with default page 0 and size 14")
    @GetMapping("/recent")
    public ResponseEntity<Page<Product>> newProducts(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "14") int size){
        return ResponseEntity.ok(homeService.newProducts(page,size));
    }
    //user
    @Operation(summary = "Get all categories Name")
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategory(){
        return ResponseEntity.ok(homeService.getCategories());
    }
    @Operation(summary = "Get all category Products",description = "Required category name in PathVariable and RequestParam with default page 0 and size 14")
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<Product>> getCategoryProducts(@PathVariable(required = true) String category,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "14") int size){
        return ResponseEntity.ok(homeService.getCategoriesProducts(category,page,size));
    }
    @Operation(summary = "Get all trending Products",description = "Take RequestParam with default page 0 and size 14")
    @GetMapping("/trending")
    public ResponseEntity<Page<Product>> getTrendingProducts(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "14") int size){
        return ResponseEntity.ok(homeService.trending(page,size));
    }

}
