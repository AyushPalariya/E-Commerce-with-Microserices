package com.example.ProductService.Service;

import com.example.ProductService.Dto.ColorDto;
import com.example.ProductService.Dto.Product;
import com.example.ProductService.Dto.SizeDto;
import com.example.ProductService.Dto.VerifyProduct;
import com.example.ProductService.Entities.Colors;
import com.example.ProductService.Entities.Gender;
import com.example.ProductService.Entities.Products;
import com.example.ProductService.Entities.Sizes;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductService {
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private InventoryFeign inventoryFeign;
    @Autowired
    private CacheManager cacheManager;
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", allEntries = true),
            @CacheEvict(value = "allProducts", allEntries = true),
            @CacheEvict(value = "newProducts",allEntries = true),
            @CacheEvict(value = "categoriesName",allEntries = true),
            @CacheEvict(value = "trending",allEntries = true),
            @CacheEvict(value = "categoryProducts",allEntries = true),
            @CacheEvict(value = "searchProducts",allEntries = true),
            @CacheEvict(value = "recentlyProducts",allEntries = true)
    })
    public List<Product> addProduct(List<Product> products) {
        if (products == null || products.isEmpty()) throw new BadRequestException("At least one product is required!");
        List<Products> pp=new ArrayList<>();
        for (Product product : products) {
            List<ColorDto> colors = product.getColors();
            if(colors==null||colors.isEmpty()) throw new BadRequestException("Color details should be required!");
            for(ColorDto colorDto:colors){
                if(colorDto.getImages()==null||colorDto.getImages().isBlank()) throw new BadRequestException("Image is required for "+product.getName());
                if(colorDto.getColorOrderSequence()==null||colorDto.getColorOrderSequence()<=0) throw new BadRequestException("Sequence is required for "+product.getName());
                if(colorDto.getColor()==null||colorDto.getColor().isBlank()) throw new BadRequestException("Color name is Required for "+product.getName());
                if(colorDto.getSizes().stream().anyMatch(s->s.getAmount()==null||s.getAmount()<=0
                ||s.getSize()==null||s.getSize().isBlank())) throw new BadRequestException("Required Amount or Size!");
            }
            if (product == null) throw new BadRequestException("Product details are required!");
            if (product.getCategory() == null || product.getCategory().isBlank())
                throw new BadRequestException("category is required!");
            if (product.getDescription() == null || product.getDescription().isBlank())
                throw new BadRequestException("Description is required!");
            if (product.getName() == null || product.getName().isBlank())
                throw new BadRequestException("name is required!");
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
            @CacheEvict(value = "searchProducts",allEntries = true),
            @CacheEvict(value = "recentlyProducts",allEntries = true)
    })
    @Transactional
    public Product updateProduct(Long id, Map<String,Object> mp){
        if (id == null || id <= 0) throw new BadRequestException("Product id must be positive!");
        if (mp == null || mp.isEmpty()) throw new BadRequestException("Product fields are required!");
        Products p= productRepo.findById(id).orElseThrow(()->new ResourceNotFound("Product not found with id: "+id));
        for (Map.Entry<String, Object> entry : mp.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            switch (key) {
                case "description" -> p.setDescription(asRequiredString(value, key));
                case "category" -> p.setCategory(asRequiredString(value, key));
                case "name" -> p.setName(asRequiredString(value, key));
                case "colors" -> replaceColors(p, value);
                case "gender" -> p.setGender(asGender(value));
                case "discountPercentage" -> {
                    p.setDiscountPercentage(asLong(value, key));
                    if(p.getDiscountPercentage()>=15) p.setOnSale(true);
                    else p.setOnSale(false);
                }
                case "avgRating" -> p.setAvgRating(asDouble(value, key));
                default -> throw new BadRequestException("Unsupported product field: " + key);
            }
        }
        if (p.getCategory() == null || p.getCategory().isBlank()
                || p.getDescription() == null || p.getDescription().isBlank()
                || p.getName() == null || p.getName().isBlank())
            throw new BadRequestException("Category, description, and name are required!");

        if (p.getDiscountPercentage() != null
                && (p.getDiscountPercentage() < 0 || p.getDiscountPercentage() > 100))
            throw new BadRequestException("Discount percentage must be between 0 and 100!");
        if (p.getAvgRating() != null && (!Double.isFinite(p.getAvgRating()) || p.getAvgRating() < 0))
            throw new BadRequestException("Average rating must be a non-negative number!");
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
            @CacheEvict(value = "searchProducts",allEntries = true),
            @CacheEvict(value = "recentlyProducts",allEntries = true)
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
            @CacheEvict(value = "searchProducts",allEntries = true),
            @CacheEvict(value = "recentlyProducts",allEntries = true)
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
        product.setName(p.getName());
        product.setAvgRating(p.getAvgRating());
        product.setGender(String.valueOf(p.getGender()));
        product.setDiscountPercentage(p.getDiscountPercentage());
        product.setOnSale(p.getOnSale());

        List<ColorDto> colors = p.getColors() == null
                ? new ArrayList<>()
                : p.getColors().stream().map(color -> {
                    ColorDto colorDto = new ColorDto();
                    colorDto.setColorId(color.getId());
                    colorDto.setColor(color.getColor());
                    colorDto.setColorOrderSequence(color.getColorOrderSequence());
                    colorDto.setImages(color.getImages());

                    List<SizeDto> sizes = color.getSizes() == null
                            ? new ArrayList<>()
                            : color.getSizes().stream().map(size -> {
                                SizeDto sizeDto = new SizeDto();
                                sizeDto.setSizeId(size.getId());
                                sizeDto.setSize(size.getSize());
                                sizeDto.setAmount(size.getAmount());
                                return sizeDto;
                            }).toList();
                    colorDto.setSizes(sizes);
                    return colorDto;
                }).toList();
        product.setColors(colors);

        return product;
    }

    private Products toEntity(Product product) {
        Products entity = new Products();
        entity.setCategory(product.getCategory());
        entity.setDescription(product.getDescription());
        entity.setName(product.getName());
        entity.setAvgRating(product.getAvgRating());
        entity.setGender(Gender.valueOf(product.getGender()));
        Long disPer=product.getDiscountPercentage();
        entity.setDiscountPercentage(disPer);
        entity.setOnSale(disPer >= 15);
        List<Colors> colors = new ArrayList<>();
        for(ColorDto cd:product.getColors()){
            Colors cc=new Colors();
            cc.setColorOrderSequence(cd.getColorOrderSequence());
            cc.setImages(cd.getImages());
            cc.setColor(cd.getColor());
            List<Sizes> sizes=new ArrayList<>();
            for(SizeDto sd:cd.getSizes()){
                 Sizes ss=new Sizes();
                 ss.setAmount(sd.getAmount());
                 ss.setSize(sd.getSize());
                 ss.setColors(cc);
                 sizes.add(ss);
            }
            cc.setProducts(entity);
            cc.setSizes(sizes);
            colors.add(cc);
        }
        entity.setColors(colors);
        return entity;
    }



    private String asRequiredString(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank())
            throw new BadRequestException(field + " must be a non-blank string!");
        return text;
    }

    private List<Colors> asColors(Object value, Products product) {
        if (!(value instanceof List<?> values))
            throw new BadRequestException("colors must be a list!");

        List<Colors> colors = new ArrayList<>();
        for (Object item : values) {
            if (!(item instanceof Map<?, ?> colorData))
                throw new BadRequestException("Each color must be an object!");

            Colors color = new Colors();
            color.setColor(asRequiredString(colorData.get("color"), "color"));
            color.setImages(asRequiredString(colorData.get("images"), "images"));
            Long sequence = asLong(colorData.get("colorOrderSequence"), "colorOrderSequence");
            if (sequence == null || sequence <= 0)
                throw new BadRequestException("colorOrderSequence must be positive!");
            color.setColorOrderSequence(sequence);
            color.setProducts(product);

            Object sizesValue = colorData.get("sizes");
            if (!(sizesValue instanceof List<?> sizeValues) || sizeValues.isEmpty())
                throw new BadRequestException("Each color must have at least one size!");
            List<Sizes> sizes = new ArrayList<>();
            for (Object sizeItem : sizeValues) {
                if (!(sizeItem instanceof Map<?, ?> sizeData))
                    throw new BadRequestException("Each size must be an object!");
                Sizes size = new Sizes();
                size.setSize(asRequiredString(sizeData.get("size"), "size"));
                Double amount = asDouble(sizeData.get("amount"), "amount");
                if (amount == null || !Double.isFinite(amount) || amount <= 0)
                    throw new BadRequestException("Size amount must be a positive number!");
                size.setAmount(amount);
                size.setColors(color);
                sizes.add(size);
            }
            color.setSizes(sizes);
            colors.add(color);
        }
        return colors;
    }

    private void replaceColors(Products product, Object value) {
        List<Colors> colors = asColors(value, product);
        if (product.getColors() == null) {
            product.setColors(colors);
        } else {
            product.getColors().clear();
            product.getColors().addAll(colors);
        }
    }

    private Boolean asBoolean(Object value, String field) {
        if (!(value instanceof Boolean booleanValue))
            throw new BadRequestException(field + " must be true or false!");
        return booleanValue;
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
            @CacheEvict(value = "searchProducts",allEntries = true),
            @CacheEvict(value = "recentlyProducts",allEntries = true)
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
