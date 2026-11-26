package com.example.ProductService.Controller;

import com.example.ProductService.Dto.Product;
import com.example.ProductService.Dto.VerifyProduct;
import com.example.ProductService.Entities.Products;
import com.example.ProductService.Service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hibernate.annotations.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@Tag(name = "Products APIs",description = "RestApis for managing Products")
public class ProductController {
    @Autowired
    private ProductService productService;


    @Operation(summary = "Get Product by searching",description = "Take RequestParam with matching keywords, default page 0 and size 20")
    @GetMapping("/search")
    public ResponseEntity<Page<Product>> searchProduct(@RequestParam String keyword,@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size){
        return ResponseEntity.ok().body(productService.search(keyword,page,size));
    }
    //admin
    @Operation(summary = "Add new Product",description = "Give accessToken of admin in Authorization and ProductId,SizeId and ColorId and OnSale not send when adding Products")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/add")
    public ResponseEntity<List<Product>> addProduct(@RequestBody List<Product> product){
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(product));
    }
    @Operation(summary = "Get all Products",description = "Products list")
    @GetMapping("/getAll")
    public ResponseEntity<Page<Product>> getAll(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size){
        return ResponseEntity.status(HttpStatus.OK).body(productService.getAllProduct(page,size));
    }
    @Operation(summary = "Get Product by id",description = "Give id as a PathVariable")
    @GetMapping("/get/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id){
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProduct(id));
    }
    //admin
    @Operation(summary = "Update Product by Id",description = "Give accessToken of admin in Authorization",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Product fields to update",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Update Product",
                                    value = """
                {
                    "description": "Premium cotton casual shirt",
                    "category": "Men",
                    "img": "https://example.com/shirt.jpg",
                    "name": "Casual Shirt",
                    "amount": 1499,
                    "size": "L",
                    "color": "Black",
                    "gender": "Male",
                    "discountPercentage": 20,
                    "avgRating": 4.5
                }
                """
                            )
                    ))
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/update/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Map<String,Object> mp){
        return new ResponseEntity<>(productService.updateProduct(id,mp),HttpStatus.OK);
    }
    //admin
    @Operation(summary = "Delete all Products",description = "Give accessToken of admin in Authorization")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/deleteAll")
    public ResponseEntity<Void> deleteAll(){
        productService.deleteAll();
        return ResponseEntity.noContent().build();
    }
    //admin
    @Operation(summary = "Delete Products",description = "Give ids as a list and give accessToken of admin in Authorization")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String ,String>> delete(@RequestBody List<Long> id){
        return new ResponseEntity<>(productService.deleteProduct(id),HttpStatus.OK);
    }
    //admin
    @Operation(summary = "clear cache")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/deleteCache")
    public ResponseEntity<Void> deleteCacheData(){
        productService.clearProductCache();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @Operation(summary = "Order Service call this Api internally")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/bulk")
    public ResponseEntity<List<Product>> getProductwithIds(@RequestBody List<Long> ids){
        return ResponseEntity.ok().body(productService.getAllProductsById(ids));
    }
    @Operation(summary = "Inventory Service call internally this api")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/verify")
    public ResponseEntity<List<VerifyProduct>> verifyFromInventory(@RequestBody List<VerifyProduct> products){
        return new ResponseEntity<>(productService.verify(products),HttpStatus.OK);
    }
}
