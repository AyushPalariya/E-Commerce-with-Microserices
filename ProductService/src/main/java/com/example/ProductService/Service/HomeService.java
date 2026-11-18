package com.example.ProductService.Service;

import com.example.ProductService.Dto.Product;
import com.example.ProductService.Entities.Products;
import com.example.ProductService.ExceptionHandle.BadRequestException;
import com.example.ProductService.Repository.InventoryFeign;
import com.example.ProductService.Repository.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HomeService {
    @Autowired
    private ProductRepo productRepo;
    @Autowired(required = true)
    private InventoryFeign inventoryFeign;


    @Transactional(readOnly = true)
    public Page<Product> newProducts(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new BadRequestException("Invalid pagination parameters!");
        }
        size=Math.min(size,14);
        Pageable pageable=PageRequest.of(page,size);
        Page<Products> result=productRepo.findAllByOrderByCreatedDesc(pageable);
        if(result.isEmpty()||page>=result.getTotalPages())  throw new BadRequestException("Page does not exist!");
        return result.map(this::toDto);
    }


    public List<String> getCategories() {
        return productRepo.findAllByCategories();
    }

    @Transactional(readOnly = true)
    public Page<Product> trending(int page, int size) {

        if (page < 0 || size <= 0) {
            throw new BadRequestException(
                    "Invalid pagination parameters!"
            );
        }

        size = Math.min(size, 14);
        Pageable pageable = PageRequest.of(page, size);

        List<Long> ids = inventoryFeign.getProductIdsWithMaxSold();

        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Page does not exist!");
        }

        List<Products> products = productRepo.findAllById(ids);
        Map<Long, Products> productMap = products.stream()
                .collect(Collectors.toMap(
                        Products::getId,
                        Function.identity()
                ));

        List<Product> sortedProducts = ids.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .map(this::toDto)
                .toList();
        int start = page * size;
        int end = Math.min(start + size, sortedProducts.size());
        List<Product> pageContent = start >= sortedProducts.size()
                ? List.of()
                : sortedProducts.subList(start, end);
        Page<Product> result = new PageImpl<>(pageContent, pageable, sortedProducts.size());
        if (page >= result.getTotalPages()) {
            throw new BadRequestException("Page does not exist!");
        }
        return result;
    }

    private Product toDto(Products p) {
        Product product = new Product();
        product.setId(p.getId());
        product.setCategory(p.getCategory());
        product.setDescription(p.getDescription());
        product.setImg(p.getImg());
        product.setAmount(p.getAmount());
        product.setName(p.getName());
        product.setColor(p.getColor());
        product.setSize(p.getSize());
        product.setAvgRating(p.getAvgRating());
        product.setGender(p.getGender());
        product.setDiscountPercentage(p.getDiscountPercentage());
        product.setDiscountAmount(p.getDiscountAmount());
        return product;
    }
}
