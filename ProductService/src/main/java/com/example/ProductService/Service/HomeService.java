package com.example.ProductService.Service;

import com.example.ProductService.Dto.Product;
import com.example.ProductService.Entities.Products;
import com.example.ProductService.ExceptionHandle.BadRequestException;
import com.example.ProductService.ExceptionHandle.ProductIdsNotFound;
import com.example.ProductService.Repository.InventoryFeign;
import com.example.ProductService.Repository.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HomeService {
    @Autowired
    private ProductRepo productRepo;
    @Autowired(required = true)
    private InventoryFeign inventoryFeign;
    @Autowired
    private CacheManager cacheManager;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "newProducts",
            key = "#page + '-' + T(java.lang.Math).min(#size, 14)"
    )
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

    @Cacheable(value = "categoriesName", key = "'all'")
    public List<String> getCategories() {
        return productRepo.findAllByCategories();
    }
    @Cacheable(
            value = "trending",
            key = "#page + '-' + T(java.lang.Math).min(#size, 14)"
    )
    @Transactional(readOnly = true)
    public Page<Product> trending(int page, int size) {

        if (page < 0 || size <= 0) {
            throw new BadRequestException(
                    "Invalid pagination parameters!"
            );
        }

        size = Math.min(size, 14);
        Pageable pageable = PageRequest.of(page, size);

        List<Long> ids = inventoryFeign.getProductIdsWithMaxSold().getBody();

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
    @Transactional
    @Cacheable(
            value = "categoryProducts",
            key = "#category.toLowerCase() + '-' + #page + '-' + T(java.lang.Math).min(#size, 14)"
    )
    public Page<Product> getCategoriesProducts(String category,int page,int size) {
        if(category==null||category.isBlank()) throw new BadRequestException("Please provide category name!");
        if (page < 0 || size <= 0) {
            throw new BadRequestException("Invalid pagination parameters!");
        }
        size=Math.min(size,14);
        Pageable pageable=PageRequest.of(page,size);
        Page<Products> result=productRepo.findAllByCategoryIgnoreCase(category,pageable);
        if(result.isEmpty()||page>=result.getTotalPages()) throw new BadRequestException("Page does not exist!");
        return result.map(this::toDto);
    }

    @Transactional
    @CachePut(value = "recentlyProducts",key = "#userId")
    public List<Product> addRecentlyProduct(Long productId, Long userId) {
        if (userId == null || userId <= 0)
            throw new BadRequestException("Invalid user ID!");

        if (productId == null || productId <= 0)
            throw new BadRequestException("Invalid product ID!");
        Products product = productRepo.findById(productId).orElseThrow(() -> new ProductIdsNotFound("Product not found with id:" + productId));
        Cache cache = cacheManager.getCache("recentlyProducts");
        List<Product> list = cache.get(userId, List.class);
        list=list==null?new ArrayList<>():new ArrayList<>(list);
        Optional<Product> first = list.stream().filter(p -> p.getId().equals(productId)).findFirst();
        if(first.isPresent()) {
            list.remove(first.get());
            list.add(0, first.get());
        }
        else {
            Product dto = toDto(product);
            list.add(0,dto);
        }
        if(list.size()>10){
            list.remove(list.size()-1);
        }
        return list;

    }
    @Transactional
    @Cacheable(value = "recentlyProducts",key = "#userId")
    public List<Product> getRecentlyView(Long userId) {
        if (userId == null || userId <= 0)
            throw new BadRequestException("Invalid user ID!");

        return new ArrayList<>();
    }
}
