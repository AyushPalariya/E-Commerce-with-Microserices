package com.example.ProductService.Service;

import com.example.ProductService.Dto.Product;
import com.example.ProductService.Dto.VerifyProduct;
import com.example.ProductService.Entities.Gender;
import com.example.ProductService.Entities.Products;
import com.example.ProductService.ExceptionHandle.BadRequestException;
import com.example.ProductService.ExceptionHandle.ConflictException;
import com.example.ProductService.ExceptionHandle.ProductIdsNotFound;
import com.example.ProductService.ExceptionHandle.ResourceNotFound;
import com.example.ProductService.Repository.InventoryFeign;
import com.example.ProductService.Repository.ProductRepo;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductService {
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private InventoryFeign inventoryFeign;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", allEntries = true),
            @CacheEvict(value = "allProducts", allEntries = true),
            @CacheEvict(value = "newProducts",allEntries = true),
            @CacheEvict(value = "categoriesName",allEntries = true),
            @CacheEvict(value = "trending",allEntries = true),
            @CacheEvict(value = "categoryProducts",allEntries = true),
            @CacheEvict(value = "searchProducts",allEntries = true)
    })
    public List<Product> addProduct(List<Product> products) {
        if (products == null || products.isEmpty()) throw new BadRequestException("At least one product is required!");
        List<Products> pp=new ArrayList<>();
        for (Product product : products) {
            if (product == null) throw new BadRequestException("Product details are required!");
            if (product.getAmount() == null || !Double.isFinite(product.getAmount()) || product.getAmount() < 0)
                throw new BadRequestException("Amount must be a non-negative number!");
            if (product.getCategory() == null || product.getCategory().isBlank())
                throw new BadRequestException("category is required!");
            if (product.getDescription() == null || product.getDescription().isBlank())
                throw new BadRequestException("Description is required!");
            if (product.getName() == null || product.getName().isBlank())
                throw new BadRequestException("name is required!");
            if (product.getImg() == null || product.getImg().isBlank())
                throw new BadRequestException("img is required!");
            validateOptions(product.getColor(), "color");
            validateOptions(product.getSize(), "size");
            if (product.getDiscountPercentage() != null
                    && (product.getDiscountPercentage() < 0 || product.getDiscountPercentage() > 100))
                throw new BadRequestException("Discount percentage must be between 0 and 100!");
            pp.add(toEntity(product));
        }
        List<Products> allProducts=productRepo.saveAll(pp);
        return allProducts.stream().map(this::toDto).toList();
    }

    @Cacheable(value = "product",key="#id")
    @Transactional
    public Product getProduct(Long id){
        if (id == null || id <= 0) throw new BadRequestException("Product id must be positive!");
        Products p= productRepo.findById(id).orElseThrow(()->new ResourceNotFound("Product not found with id: "+id));
        return toDto(p);
    }
    @Caching(evict = {
            @CacheEvict(value = "product", allEntries = true),
            @CacheEvict(value = "allProducts", allEntries = true),
            @CacheEvict(value = "newProducts",allEntries = true),
            @CacheEvict(value = "categoriesName",allEntries = true),
            @CacheEvict(value = "trending",allEntries = true),
            @CacheEvict(value = "categoryProducts",allEntries = true),
            @CacheEvict(value = "searchProducts",allEntries = true)
    })
    @Transactional
    public Product updateProduct(Long id, Map<String,Object> mp){
        if (mp == null || mp.isEmpty()) throw new BadRequestException("Product fields are required!");
        Products p= productRepo.findById(id).orElseThrow(()->new ResourceNotFound("Product not found with id: "+id));
        for (Map.Entry<String, Object> entry : mp.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            switch (key) {
                case "description" -> p.setDescription((String) value);
                case "category" -> p.setCategory((String) value);
                case "img" -> p.setImg((String) value);
                case "amount" -> p.setAmount(asDouble(value, key));
                case "name" -> p.setName((String) value);
                case "size" -> p.setSize(asStringList(value, key));
                case "color" -> p.setColor(asStringList(value, key));
                case "gender" -> p.setGender(asGender(value));
                case "discountPercentage" ->{
                    p.setDiscountPercentage(asLong(value, key));
                    double disAmount=(p.getAmount()*p.getDiscountPercentage())/100.0;
                    p.setDiscountAmount(disAmount);
                }
                case "avgRating" -> p.setAvgRating(asDouble(value, key));
                default -> throw new BadRequestException("Unsupported product field: " + key);
            }
        }
        if (p.getAmount() == null || !Double.isFinite(p.getAmount()) || p.getAmount() < 0)
            throw new BadRequestException("Amount must be a non-negative number!");
        if (p.getCategory() == null || p.getCategory().isBlank()
                || p.getDescription() == null || p.getDescription().isBlank()
                || p.getName() == null || p.getName().isBlank()
                || p.getImg() == null || p.getImg().isBlank())
            throw new BadRequestException("Category, description, name, and image are required!");
        validateOptions(p.getColor(), "color");
        validateOptions(p.getSize(), "size");
        if (p.getDiscountPercentage() != null
                && (p.getDiscountPercentage() < 0 || p.getDiscountPercentage() > 100))
            throw new BadRequestException("Discount percentage must be between 0 and 100!");
        p=productRepo.save(p);
        return toDto(p);
    }

    @Caching(evict = {
            @CacheEvict(value = "product", allEntries = true),
            @CacheEvict(value = "allProducts", allEntries = true),
            @CacheEvict(value = "newProducts",allEntries = true),
            @CacheEvict(value = "categoriesName",allEntries = true),
            @CacheEvict(value = "trending",allEntries = true),
            @CacheEvict(value = "categoryProducts",allEntries = true),
            @CacheEvict(value = "searchProducts",allEntries = true)
    })
    @Transactional
    public Map<String,String> deleteProduct(List<Long> ids){
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Product IDs cannot be empty!");
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BadRequestException("Product IDs must be positive!");
        }
        List<Long> uniqueIds = ids.stream()
                .distinct()
                .toList();
        for(Long id:uniqueIds){
            if(!productRepo.existsById(id)) throw new BadRequestException("Product with id "+id+" not found!");
        }
        try {
            inventoryFeign.deleteInventoryDataWithIds(uniqueIds).getBody();
            log.info("inventory data deleted");
        }
        catch (FeignException e){
            throw new BadRequestException("Unable to delete data from inventory service");
        }
        productRepo.deleteAllById(uniqueIds);
        System.out.println("product deleted!");
        return Map.of("SUCCESS","Product is successfully deleted.");
    }
    @Cacheable(value = "allProducts", key = "#page + '-' + T(java.lang.Math).min(#size, 20)")
    @Transactional
    public Page<Product> getAllProduct(int page,int size){
        if (page < 0 || size <= 0) {
            throw new BadRequestException("Invalid pagination parameters!");
        }
        size=Math.min(size,20);
        Pageable pageable=PageRequest.of(page,size);
        Page<Products> result=productRepo.findAll(pageable);
        if(result.isEmpty()||result.getTotalPages()<=page) throw new BadRequestException("Page does not exist");
        return result.map(this::toDto);
    }

    @Caching(evict = {
            @CacheEvict(value = "product", allEntries = true),
            @CacheEvict(value = "allProducts", allEntries = true),
            @CacheEvict(value = "newProducts",allEntries = true),
            @CacheEvict(value = "categoriesName",allEntries = true),
            @CacheEvict(value = "trending",allEntries = true),
            @CacheEvict(value = "categoryProducts",allEntries = true),
            @CacheEvict(value = "searchProducts",allEntries = true)
    })
    public void clearProductCache() {
        System.out.println("Product cache cleared!");
    }

    @Transactional
    public List<Product> getAllProductsById(List<Long> ids) {
        List<Products> allById = productRepo.findAllById(ids);
        return allById.stream().map(this::toDto).toList();
    }
    @Transactional
    public List<VerifyProduct> verify(List<VerifyProduct> products) {
        if (products == null || products.isEmpty()) throw new BadRequestException("At least one product is required!");
        if (products.stream().anyMatch(product -> product == null || product.getId() == null || product.getId() <= 0
                || product.getName() == null || product.getName().isBlank()))
            throw new BadRequestException("Each product must have a valid id and name!");
        List<Long> ids=products.stream().map(VerifyProduct::getId).toList();
        List<VerifyProduct> verify=productRepo.findVerifyProducts(ids);
        Map<Long,String> mp=verify.stream().collect(Collectors.toMap(VerifyProduct::getId,VerifyProduct::getName));
        for(VerifyProduct p:products){
            if(!p.getName().equalsIgnoreCase(mp.get(p.getId()))){
                throw new ProductIdsNotFound("Invalid product ID or name: "+p.getId()+"-"+p.getName());
            }
        }
        return verify;
    }
    @Transactional
    @Cacheable(
            value = "searchProducts",
            key = "#keyword.trim().toLowerCase() + '-' + #page + '-' + T(java.lang.Math).min(#size, 20)"
    )
    public Page<Product> search(String keyword, int page, int size){
        if(keyword==null||keyword.isBlank()) throw new BadRequestException("Please search something!");
        if (page < 0 || size <= 0) {
            throw new BadRequestException("Invalid pagination parameters!");
        }
        size=Math.min(size,20);
        Pageable pageable= PageRequest.of(page,size, Sort.by("avgRating").descending().and(Sort.by("amount").ascending()));
        Page<Products> result=
                productRepo.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, keyword, pageable);
        if(result.isEmpty()||page>=result.getTotalPages())  throw new BadRequestException("Page does not exist!");
        return result.map(this::toDto);
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

    private Products toEntity(Product product) {
        Products entity = new Products();
        entity.setCategory(product.getCategory());
        entity.setDescription(product.getDescription());
        entity.setImg(product.getImg());
        double amount=product.getAmount();
        entity.setAmount(amount);
        entity.setName(product.getName());
        entity.setColor(product.getColor());
        entity.setSize(product.getSize());
        entity.setAvgRating(product.getAvgRating());
        entity.setGender(product.getGender());
        Long disPer=product.getDiscountPercentage();
        entity.setDiscountPercentage(disPer);
        double disAmount=(disPer*amount)/100.0;
        entity.setDiscountAmount(disAmount);
        return entity;
    }

    private void validateOptions(List<String> options, String field) {
        if (options == null || options.isEmpty()
                || options.stream().anyMatch(option -> option == null || option.isBlank()))
            throw new BadRequestException(field + " must contain at least one non-blank value!");
    }

    private List<String> asStringList(Object value, String field) {
        if (!(value instanceof List<?> values)
                || values.stream().anyMatch(item -> !(item instanceof String)))
            throw new BadRequestException(field + " must be a list of strings!");
        return values.stream().map(String.class::cast).toList();
    }

    private Double asDouble(Object value, String field) {
        if (value == null) return null;
        if (!(value instanceof Number number)) throw new BadRequestException(field + " must be numeric!");
        return number.doubleValue();
    }

    private Long asLong(Object value, String field) {
        if (value == null) return null;
        if (!(value instanceof Number number)) throw new BadRequestException(field + " must be numeric!");
        return number.longValue();
    }

    private Gender asGender(Object value) {
        if (value == null) return null;
        if (value instanceof Gender gender) return gender;
        if (!(value instanceof String gender)) throw new BadRequestException("gender must be MALE or FEMALE!");
        try {
            return Gender.valueOf(gender.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("gender must be MALE or FEMALE!");
        }
    }
    @Caching(evict = {
            @CacheEvict(value = "product", allEntries = true),
            @CacheEvict(value = "allProducts", allEntries = true),
            @CacheEvict(value = "newProducts",allEntries = true),
            @CacheEvict(value = "categoriesName",allEntries = true),
            @CacheEvict(value = "trending",allEntries = true),
            @CacheEvict(value = "categoryProducts",allEntries = true),
            @CacheEvict(value = "searchProducts",allEntries = true)
    })
    @Transactional
    public void deleteAll() {
        try {
            inventoryFeign.deleteAllInventoryData().getBody();
            log.info("Inventory data deleted successfully");
        } catch (FeignException ex) {
            log.error("Failed to delete inventory data: {}", ex.getMessage());
            throw new BadRequestException(
                    "Unable to delete inventory data. Products were not deleted."
            );
        }
        productRepo.deleteAll();
        log.info("deleteAll Products");
    }
}
