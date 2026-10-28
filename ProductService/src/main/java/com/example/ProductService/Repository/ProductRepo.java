package com.example.ProductService.Repository;

import com.example.ProductService.Entities.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepo extends JpaRepository<Products,Long> {
    Boolean existsByName(String name);
}
