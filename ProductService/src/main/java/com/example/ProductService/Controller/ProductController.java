package com.example.ProductService.Controller;

import com.example.ProductService.Dto.Product;
import com.example.ProductService.Dto.VerifyProduct;
import com.example.ProductService.Entities.Products;
import com.example.ProductService.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductService productService;

    @PostMapping("/add")
    public ResponseEntity<List<Product>> addProduct(@RequestBody List<Product> product){
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(product));
    }
    @GetMapping("/getAll")
    public ResponseEntity<List<Product>> getAll(){
        return ResponseEntity.status(HttpStatus.OK).body(productService.getAllProduct());
    }
    @GetMapping("/get/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id){
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProduct(id));
    }
    @PatchMapping("/update/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Map<String,Object> mp){
        return new ResponseEntity<>(productService.updateProduct(id,mp),HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String ,String>> delete(@PathVariable Long id){
        return new ResponseEntity<>(productService.deleteProduct(id),HttpStatus.OK);
    }
    @GetMapping("/deleteCache")
    public ResponseEntity<Void> deleteCacheData(){
        productService.clearProductCache();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @PostMapping("/bulk")
    public ResponseEntity<List<Product>> getProductwithIds(@RequestBody List<Long> ids){
        return ResponseEntity.ok().body(productService.getAllProductsById(ids));
    }
    @PostMapping("/verify")
    public ResponseEntity<List<VerifyProduct>> verifyFromInventory(@RequestBody List<VerifyProduct> products){
        return new ResponseEntity<>(productService.verify(products),HttpStatus.OK);
    }
}
