package com.example.InventoryService.Service;

import com.example.InventoryService.Dto.Inventory;
import com.example.InventoryService.Dto.InventoryItem;
import com.example.InventoryService.Dto.Product;
import com.example.InventoryService.Entities.InventoryEntity;
import com.example.InventoryService.Global.BadRequestExceptionClass;
import com.example.InventoryService.Repository.FeignClientInterface;
import com.example.InventoryService.Repository.InventoryRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InventoryService {
    @Autowired
    private InventoryRepo inventoryRepo;
    @Autowired
    private FeignClientInterface feignClientInterface;
    @Transactional
    @CacheEvict(value = {"allInventory","inventory"},allEntries = true)
    public List<Inventory> add(List<Inventory> inventorys) {
        validate(inventorys);
        List<InventoryEntity> list=new ArrayList<>();
        inventorys.forEach(in->{
            InventoryEntity inventoryEntity=new InventoryEntity();
            inventoryEntity.setName(in.getName());
            inventoryEntity.setProductId(in.getProductId());
            inventoryEntity.setTotalQuantity(in.getTotalQuantity());
            list.add(inventoryEntity);
        });
        List<InventoryEntity> inventoryEntities = inventoryRepo.saveAll(list);
        log.info("inventory added");
        return convertIntoDto(inventoryEntities);
    }

    private List<Inventory> convertIntoDto(List<InventoryEntity> inventoryEntities) {
        return inventoryEntities.stream().map(en->new Inventory(en.getId(),en.getName(),en.getProductId(),en.getTotalQuantity()))
                .toList();
    }

    private void validate(List<Inventory> inventorys) {
        List<Product> productList=new ArrayList<>();
        inventorys.forEach(inventory->{
            if(inventory.getName()==null||inventory.getName().isBlank()) throw new BadRequestExceptionClass("name is required!");
            if(inventory.getProductId()==null||inventory.getProductId()<=0) throw new BadRequestExceptionClass("productId must be valid!");
            if(inventory.getTotalQuantity()==null||inventory.getTotalQuantity()<=0) throw new BadRequestExceptionClass("totalQuantity is required!");
            productList.add(new Product(inventory.getProductId(),inventory.getName()));
        });
        List<Product> products=feignClientInterface.verifyProductIds(productList);
        log.info("products are :{}",products);
    }
    @Transactional
    @CacheEvict(value = {"allInventory","inventory"},allEntries = true)
    public void delete(List<Long> ids) {
        if(ids==null||ids.isEmpty()) throw new BadRequestExceptionClass("ids must be entered by you!");
        inventoryRepo.deleteAllById(ids);
        log.info("inventorys deleted:{}",ids);
    }

    public void reserved(Long orderId, List<InventoryItem> items) {

    }
    @Transactional
    @Cacheable(value = "allInventory",key = "'all'")
    public List<Inventory> getAllStock() {
        List<InventoryEntity> all = inventoryRepo.findAll();
        log.info("allStocks of inventory get..");
        //dto
        return all.stream().map(in->new Inventory(in.getId(),in.getName(),in.getProductId(),
                in.getTotalQuantity(),in.getAvailableQuantity(),in.getReservedQuantity(),in.getSoldQuantity()))
                .toList();
    }
    @Transactional
    @Cacheable(value = "inventory",key = "#inventory.productId")
    public Inventory getStock(Inventory inventory){
        InventoryEntity in = inventoryRepo.findByProductId(inventory.getProductId()).orElseThrow(() -> new BadRequestExceptionClass("product not found with productId:" + inventory.getProductId()));
        log.info("inventory get with productId:{}",in.getProductId());
        return new Inventory(in.getId(),in.getName(),in.getProductId(),in.getTotalQuantity(),in.getAvailableQuantity(),
                in.getReservedQuantity(),in.getSoldQuantity());
    }
    @Transactional
    @CacheEvict(value = {"allInventory","inventory"},allEntries = true)
    public List<Inventory> updateInventory(List<InventoryItem> items){
        if(items==null||items.isEmpty()) throw new BadRequestExceptionClass("Stock details required!");
        List<Long> productIds=items.stream().map(update->{
            if (update.getProductId() == null || update.getProductId() <= 0)
                throw new BadRequestExceptionClass("Invalid productId");

            if (update.getQuantities()==null||update.getQuantities() <= 0)
                throw new BadRequestExceptionClass("Invalid quantity");
            return update.getProductId();
        }).toList();
        if(productIds.size()!=productIds.stream().distinct().toList().size()) throw new BadRequestExceptionClass("duplicate ids are present!");
        List<InventoryEntity> inventories = inventoryRepo.findAllByProductIdIn(productIds);
        if(inventories.size()!= productIds.size()) throw new BadRequestExceptionClass("some records are not found!");

        Map<Long,Long> mp=items.stream().collect(Collectors.toMap(InventoryItem::getProductId,InventoryItem::getQuantities));
        for(InventoryEntity in:inventories){
            Long userQuantity=mp.get(in.getProductId());
            in.setTotalQuantity(in.getTotalQuantity()+userQuantity);
            in.setAvailableQuantity(in.getAvailableQuantity()+userQuantity);
        }
        List<InventoryEntity> updated = inventoryRepo.saveAll(inventories);
        return updated.stream().map(in->new Inventory(in.getId(),in.getName(),in.getProductId(),
                        in.getTotalQuantity(),in.getAvailableQuantity(),in.getReservedQuantity(),in.getSoldQuantity()))
                .toList();
    }

}
