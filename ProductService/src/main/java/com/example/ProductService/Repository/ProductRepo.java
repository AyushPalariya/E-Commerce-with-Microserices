package com.example.ProductService.Repository;

import com.example.ProductService.Dto.VerifyProduct;
import com.example.ProductService.Entities.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Products,Long> {
    Page<Products>
    findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name,
            String category,
            String description,
            Pageable pageable
    );


    @Query("""
        Select new com.example.ProductService.Dto.VerifyProduct(p.id,p.name)
         From Products p where p.id In :productIds
""")
    List<VerifyProduct> findVerifyProducts(@Param("productIds") List<Long> productIds);

    Page<Products> findAllByOrderByCreatedDesc(Pageable pageable);
    @Query("""
            Select Distinct p.category from Products p order by  p.category
""")
    List<String> findAllByCategories();


    Page<Products> findAllByIdIn(List<Long> productIdsWithMaxSold, Pageable pageable);

    Page<Products> findAllByCategoryIgnoreCase(String category, Pageable pageable);
}
