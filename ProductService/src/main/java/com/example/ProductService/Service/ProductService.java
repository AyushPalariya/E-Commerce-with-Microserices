package com.example.ProductService.Service;

import com.example.ProductService.Dto.Product;
import com.example.ProductService.Entities.Products;
import com.example.ProductService.ExceptionHandle.BadRequestException;
import com.example.ProductService.ExceptionHandle.ConflictException;
import com.example.ProductService.ExceptionHandle.ResourceNotFound;
import com.example.ProductService.Repository.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {
    @Autowired
    private ProductRepo productRepo;
    @CacheEvict(value = "product", allEntries = true)
    public List<Product> addProduct(List<Product> products) {
        List<Products> pp=new ArrayList<>();
        for (Product product : products) {
            if (product.getAmount() == null) throw new BadRequestException("amount required!");
            if (product.getCategory() == null || product.getCategory().isBlank())
                throw new BadRequestException("category is required!");
            if (product.getDescription() == null || product.getDescription().isBlank())
                throw new BadRequestException("category is required!");
            if (product.getName() == null || product.getName().isBlank())
                throw new BadRequestException("name is required!");
            if (product.getImg() == null || product.getImg().isBlank())
                throw new BadRequestException("img is required!");
            if (productRepo.existsByName(product.getName())) throw new ConflictException("name already exist!");
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
        Products p= productRepo.findById(id).orElseThrow(()->new ResourceNotFound("Product not found with id: "+id));
        return new Product(p.getId(),p.getCategory(),p.getDescription(),p.getImg(),p.getAmount(), p.getName());
    }
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


    public Map<String,String> deleteProduct(Long id){
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

}
