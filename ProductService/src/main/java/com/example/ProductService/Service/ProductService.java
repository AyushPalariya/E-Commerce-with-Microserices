package com.example.ProductService.Service;

import com.example.ProductService.Dto.Product;
import com.example.ProductService.Dto.VerifyProduct;
import com.example.ProductService.Entities.Products;
import com.example.ProductService.ExceptionHandle.BadRequestException;
import com.example.ProductService.ExceptionHandle.ConflictException;
import com.example.ProductService.ExceptionHandle.ProductIdsNotFound;
import com.example.ProductService.ExceptionHandle.ResourceNotFound;
import com.example.ProductService.Repository.ProductRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepo productRepo;
    @CacheEvict(value = "product", allEntries = true)
    public List<Product> addProduct(List<Product> products) {
        if (products == null || products.isEmpty()) throw new BadRequestException("At least one product is required!");
        List<Products> pp=new ArrayList<>();
        Set<String> names = new HashSet<>();
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
            if (!names.add(product.getName()) || productRepo.existsByName(product.getName()))
                throw new ConflictException("Product name already exists: " + product.getName());
            Products p = new Products(product.getCategory(), product.getDescription(), product.getImg(), product.getAmount(), product.getName());
            pp.add(p);
        }
        List<Products> allProducts=productRepo.saveAll(pp);
        return allProducts.stream().map(p->new Product(p.getId(),p.getCategory(),
                p.getDescription(),p.getImg(),p.getAmount(),
                p.getName())).toList();
    }

    @Cacheable(value = "product",key="#id")
    public Product getProduct(Long id){
        if (id == null || id <= 0) throw new BadRequestException("Product id must be positive!");
        Products p= productRepo.findById(id).orElseThrow(()->new ResourceNotFound("Product not found with id: "+id));
        return new Product(p.getId(),p.getCategory(),p.getDescription(),p.getImg(),p.getAmount(), p.getName());
    }
    @CacheEvict(value = "product",allEntries = true)
    public Product updateProduct(Long id, Map<String,Object> mp){
        Products p= productRepo.findById(id).orElseThrow(()->new ResourceNotFound("Product not found with id: "+id));
        for(String key:mp.keySet()){
            if(key.equals("description")) p.setDescription((String) mp.get("description"));
            if(key.equals("category")) p.setCategory((String) mp.get("category"));
            if(key.equals("img")) p.setImg((String) mp.get("img"));
            if(key.equals("amount")) p.setAmount((Double) mp.get("amount"));
            if(key.equals("name")) p.setName((String) mp.get("name"));
        }
        p=productRepo.save(p);
        return new Product(p.getId(),p.getCategory(),p.getDescription(),p.getImg(),p.getAmount(), p.getName());
    }

    @CacheEvict(value = "product",allEntries = true)
    public Map<String,String> deleteProduct(Long id){
        if (id == null || id <= 0) throw new BadRequestException("Product id must be positive!");
        if(!productRepo.existsById(id)) throw new ResourceNotFound("Product not found with id: "+id);
        productRepo.deleteById(id);
        System.out.println("product deleted!");
        return Map.of("SUCCESS","Product is successfully deleted.");
    }
    @Cacheable(value = "product", key = "'all'")
    public List<Product> getAllProduct(){
        return productRepo.findAll().stream().map(p->new Product(
                p.getId(),p.getCategory(),p.getDescription(),p.getImg(),p.getAmount(),p.getName()
        )).toList();
    }

    @CacheEvict(value = "product", allEntries = true)
    public void clearProductCache() {
        System.out.println("Product cache cleared!");
    }

    @Transactional
    public List<Product> getAllProductsById(List<Long> ids) {
        List<Products> allById = productRepo.findAllById(ids);
        return allById.stream().map(product->new Product(product.getId(), product.getCategory(),
                product.getDescription(), product.getImg(), product.getAmount(), product.getName()))
                .toList();
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
            if(!p.getName().equals(mp.get(p.getId()))){
                throw new ProductIdsNotFound("Invalid product ID or name: "+p.getId()+"-"+p.getName());
            }
        }
        return verify;
    }
}
