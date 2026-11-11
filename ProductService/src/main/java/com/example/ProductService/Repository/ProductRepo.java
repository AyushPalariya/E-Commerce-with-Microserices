package com.example.ProductService.Repository;

import com.example.ProductService.Dto.VerifyProduct;
import com.example.ProductService.Entities.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Products,Long> {
    Boolean existsByName(String name);
    Boolean existsByNameAndIdNot(String name, Long id);

    @Query("""
        Select new com.example.ProductService.Dto.VerifyProduct(p.id,p.name)
         From Products p where p.id In :productIds
""")
    List<VerifyProduct> findVerifyProducts(@Param("productIds") List<Long> productIds);
}
