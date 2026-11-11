package com.example.OrderService.Service;

import com.example.OrderService.Dto.*;
import com.example.OrderService.Entities.OrderEntity;
import com.example.OrderService.Entities.OrderItem;
import com.example.OrderService.Entities.Status;
import com.example.OrderService.Exception.BadRequestException;

import com.example.OrderService.Repository.InventoryClient;
import com.example.OrderService.Repository.OrderRepo;
import com.example.OrderService.Repository.ProductClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class OrderService {
    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ProductClient productClient;
    @Autowired
    private InventoryClient inventoryClient;

    @CacheEvict(value = "userOrder",allEntries = true)
    @Transactional
    public OrderPlace orderPlace(OrderPlace orderPlace,String email,Long userid){
        validateOrder(orderPlace, email, userid);
        List<Long> product_ids=orderPlace.getItems().stream().map(ItemDto::getProduct_id).distinct().toList();
        List<ProductDto> products= productClient.getProducts(product_ids).getBody();

        if (products == null || products.isEmpty()) {
            throw new BadRequestException("Products not found");
        }
        log.info("item ids: {}",product_ids);
        log.info("Product get from Product Service: {}",products);

        Map<Long,ProductDto> productMap=new HashMap<>();
        for(ProductDto p:products) {
            System.out.print(p.getId()+" ");
            productMap.put(p.getId(),p);
        }
        System.out.println(productMap);


        OrderEntity order = new OrderEntity();
        order.setUserId(userid);
        order.setUserEmail(email);
        order.setUserName(orderPlace.getName());
        order.setStatus(Status.INVENTORY_PENDING);
        log.info("Order status is :{}",Status.INVENTORY_PENDING);

        List<OrderItem> items = new ArrayList<>();
        double totalAmount=0.0;
        for (ItemDto itemDto : orderPlace.getItems()) {
            ProductDto product=productMap.get(itemDto.getProduct_id());
            if (product == null) throw new BadRequestException("Product not found: " + itemDto.getProduct_id());
            double subtotal=product.getAmount()*itemDto.getQuantities();
            totalAmount+=subtotal;
            OrderItem item = new OrderItem();
            item.setProduct_id(product.getId());
            item.setProduct_name(product.getName());
            item.setQuantities(itemDto.getQuantities());
            item.setSubAmount(subtotal);
            item.setOrder(order);
            log.info(item.getOrder().toString());
            items.add(item);
        }
        order.setItems(items);
        order.setTotalAmount(totalAmount);

        OrderEntity save = orderRepo.saveAndFlush(order);
        List<InventoryItem> inv=new ArrayList<>();
        List<ItemDto> itemDto=save.getItems().stream().map(item -> {
            ItemDto savedItem = new ItemDto();
            savedItem.setId(item.getId());
            savedItem.setProduct_id(item.getProduct_id());
            savedItem.setProduct_name(item.getProduct_name());
            savedItem.setQuantities(item.getQuantities());
            savedItem.setSubAmount(item.getSubAmount());
            InventoryItem inventoryItem=new InventoryItem(item.getProduct_id(),item.getQuantities());
            inv.add(inventoryItem);
            return savedItem;
        }).toList();
        try {
            inventoryClient.reserved(save.getId(), inv);
            log.info("Inventory reserved..");
        }
        catch (FeignException.BadRequest|FeignException.Conflict e){
            log.info("Order status is :{}",Status.INVENTORY_FAILED);
            orderRepo.updateStatus(save.getId(),Status.INVENTORY_FAILED);
            throw new BadRequestException(
                    "Inventory reservation failed: " + e.getMessage()
            );
        }
        catch (Exception e) {
            throw new RuntimeException(
                    "Unable to confirm inventory reservation. " +
                            "Order requires reconciliation."
            );
        }
        orderRepo.updateStatus(save.getId(),Status.PAYMENT_PENDING);
        log.info("Order status is :{}",Status.PAYMENT_PENDING);


        OrderPlace orderPlace1 = new OrderPlace(save.getId(), save.getUserId(), save.getUserEmail(), orderPlace.getName(),
                save.getTotalAmount(), String.valueOf(save.getStatus()), save.getDatetime(), itemDto);

        return orderPlace1;
    }

    @Transactional(readOnly = true)
    public List<OrderPlace> getAllOrder() {
        List<OrderEntity> all = orderRepo.findAllOrderWithItems();
        return convertInDto(all);
    }
    @Cacheable(value = "userOrder",key = "#userId")
    @Transactional(readOnly = true)
    public List<OrderPlace> getOrderByUserId(Long userId) {
        if (userId == null || userId <= 0) throw new BadRequestException("User id must be positive!");
        List<OrderEntity> orders = orderRepo.findOrdersByUserIdWithItems(userId);
        return convertInDto(orders);
    }

    private void validateOrder(OrderPlace orderPlace, String email, Long userId) {
        if (orderPlace == null) throw new BadRequestException("Order details are required!");
        if (email == null || email.isBlank()) throw new BadRequestException("Authenticated user email is required!");
        if (userId == null || userId <= 0) throw new BadRequestException("Authenticated user id must be positive!");
        if (orderPlace.getName() == null || orderPlace.getName().isBlank())
            throw new BadRequestException("Customer name is required!");
        if (orderPlace.getItems() == null || orderPlace.getItems().isEmpty())
            throw new BadRequestException("An order must contain at least one item!");
        for (ItemDto item : orderPlace.getItems()) {
            if (item == null) throw new BadRequestException("Order item details are required!");
            if (item.getProduct_id() == null || item.getProduct_id() <= 0)
                throw new BadRequestException("Each order item must have a positive product id!");
            if (item.getQuantities() <= 0)
                throw new BadRequestException("Each order item quantity must be positive!");
        }
    }

    public List<OrderPlace> convertInDto(List<OrderEntity> all){
        List<OrderPlace> place=new ArrayList<>();
        for (OrderEntity entity:all){
            List<OrderItem> items=entity.getItems();
            OrderPlace p=new OrderPlace(entity.getId(), entity.getUserId(), entity.getUserEmail(),
                    entity.getUserName(), entity.getTotalAmount(),String.valueOf(entity.getStatus()),entity.getDatetime(),
                    items.stream().map(i->new ItemDto(i.getId(),i.getProduct_id(),i.getProduct_name(),i.getQuantities(),i.getSubAmount())).toList()  );
            place.add(p);
        }
        return place;
    }
    @CacheEvict(value = "userOrder",key = "#userId")
    public void deleteOrder(Long orderId,Long userId) {
        orderRepo.deleteById(orderId);
        System.out.println("order delete with orderId:"+orderId);
    }

    public OrderPlace getOrder(Long orderId, UserDetailsDto user) {
        OrderEntity orderEntity = orderRepo.findById(orderId).orElseThrow(() -> new BadRequestException("orderId " + orderId + " not found! "));
        if(!orderEntity.getUserId().equals(user.getUserId())) throw new BadRequestException("userId "+user.getUserId()+" is not matches with orderEntity userId !");
        if(!orderEntity.getUserEmail().equals(user.getEmail())) throw new BadRequestException("email "+user.getEmail()+" is not matches with orderEntity email!");
        if(Status.PAYMENT_PENDING!=orderEntity.getStatus()) throw new BadRequestException("status is not matches with orderEntity status!");
        List<OrderPlace> orderPlaces = convertInDto(List.of(orderEntity));
        System.out.println("Payment want this order: "+orderPlaces);
        return orderPlaces.get(0);
    }
}
