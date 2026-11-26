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

import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Caching(evict ={ @CacheEvict(value = "userOrder",allEntries = true),
                     @CacheEvict(value = "allOrders",allEntries = true)   })
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
        log.info("ProductMap :{}",productMap);


        OrderEntity order = new OrderEntity();
        order.setUserId(userid);
        order.setUserEmail(email);
        order.setUserName(orderPlace.getName());
        order.setStatus(Status.INVENTORY_PENDING);
        log.info("Order status is :{}",Status.INVENTORY_PENDING);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (ItemDto itemDto : orderPlace.getItems()) {
            ProductDto product=productMap.get(itemDto.getProduct_id());
            if (product == null) throw new BadRequestException("Product not found: " + itemDto.getProduct_id());
            ProductSizeDto selectedSize = findSelectedSize(product, itemDto.getColor(), itemDto.getSize());
            BigDecimal unitPrice = discountedUnitPrice(product, selectedSize);
            BigDecimal subtotal = unitPrice
                    .multiply(BigDecimal.valueOf(itemDto.getQuantities()))
                    .setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(subtotal);
            OrderItem item = new OrderItem();
            item.setProduct_id(product.getId());
            item.setProduct_name(product.getName());
            item.setQuantities(itemDto.getQuantities());
            item.setSubAmount(subtotal.doubleValue());
            item.setPerAmount(unitPrice.doubleValue());
            item.setColor(itemDto.getColor());
            item.setSize(itemDto.getSize());
            item.setOrder(order);
            log.info(item.getOrder().toString());
            items.add(item);
        }
        order.setItems(items);
        order.setTotalAmount(totalAmount.doubleValue());

        OrderEntity save = orderRepo.save(order);
        List<InventoryItem> inv = new ArrayList<>();
        List<ItemDto> itemDto=save.getItems().stream().map(item -> {
            ItemDto savedItem = new ItemDto();
            savedItem.setId(item.getId());
            savedItem.setProduct_id(item.getProduct_id());
            savedItem.setProduct_name(item.getProduct_name());
            savedItem.setQuantities(item.getQuantities());
            savedItem.setSubAmount(item.getSubAmount());
            savedItem.setPerAmount(item.getPerAmount());
            savedItem.setColor(item.getColor());
            savedItem.setSize(item.getSize());
            inv.add(new InventoryItem(item.getProduct_id(), item.getQuantities(), item.getSize(), item.getColor()));
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
        save.setStatus(Status.PAYMENT_PENDING);
        log.info("Order status is :{}",Status.PAYMENT_PENDING);


        OrderPlace orderPlace1 = new OrderPlace(save.getId(), save.getUserId(), save.getUserEmail(), orderPlace.getName(),
                save.getTotalAmount(), String.valueOf(save.getStatus()), save.getDatetime(), itemDto);

        return orderPlace1;
    }
    @Cacheable(value = "allOrders",key = "'all'")
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
            if (item.getQuantities() == null || item.getQuantities() <= 0)
                throw new BadRequestException("Each order item quantity must be positive!");
            if (item.getColor() == null || item.getColor().isBlank())
                throw new BadRequestException("Each order item must include a color!");
            if (item.getSize() == null || item.getSize().isBlank())
                throw new BadRequestException("Each order item must include a size!");
            item.setColor(item.getColor().trim());
            item.setSize(item.getSize().trim());
        }
    }

    private ProductSizeDto findSelectedSize(ProductDto product, String selectedColor, String selectedSize) {
        if (product.getColors() == null) {
            throw new BadRequestException("Product has no available colors: " + product.getId());
        }
        ProductColorDto color = product.getColors().stream()
                .filter(candidate -> candidate != null && candidate.getColor() != null
                        && candidate.getColor().trim().equalsIgnoreCase(selectedColor.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Selected color is unavailable for product: " + product.getId()));
        if (color.getSizes() == null) {
            throw new BadRequestException("Selected color has no available sizes for product: " + product.getId());
        }
        return color.getSizes().stream()
                .filter(size -> size != null && size.getSize() != null
                        && size.getSize().trim().equalsIgnoreCase(selectedSize.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Selected size is unavailable for product color: " + product.getId()));
    }

    private BigDecimal discountedUnitPrice(ProductDto product, ProductSizeDto selectedSize) {
        Double amount = selectedSize.getAmount();
        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new BadRequestException("Product size has an invalid price: " + product.getId());
        }

        BigDecimal listedPrice = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        Long discountPercentage = product.getDiscountPercentage();
        if (discountPercentage == null) return listedPrice;
        if (discountPercentage < 0 || discountPercentage > 100) {
            throw new BadRequestException("Product has an invalid discount percentage: " + product.getId());
        }
        BigDecimal discount = listedPrice
                .multiply(BigDecimal.valueOf(discountPercentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return listedPrice.subtract(discount);
    }

    public List<OrderPlace> convertInDto(List<OrderEntity> all){
        List<OrderPlace> place=new ArrayList<>();
        for (OrderEntity entity:all){
            List<OrderItem> items=entity.getItems();
            OrderPlace p=new OrderPlace(entity.getId(), entity.getUserId(), entity.getUserEmail(),
                    entity.getUserName(), entity.getTotalAmount(),String.valueOf(entity.getStatus()),entity.getDatetime(),
                    items.stream().map(i->new ItemDto(i.getId(),i.getProduct_id(),i.getProduct_name(),
                            i.getQuantities(),i.getSubAmount(),i.getPerAmount(),i.getColor(),i.getSize())).toList());
            place.add(p);
        }
        return place;
    }
    @Caching(evict ={ @CacheEvict(value = "userOrder",allEntries = true),
            @CacheEvict(value = "allOrders",allEntries = true)   })
    public void deleteOrder(Long orderId,Long userId) {
        orderRepo.deleteById(orderId);
        System.out.println("order delete with orderId:"+orderId);
    }

    @Transactional(readOnly = true)
    public OrderPlace getOrder(Long orderId, UserDetailsDto user) {
        OrderEntity orderEntity = orderRepo.findById(orderId).orElseThrow(() -> new BadRequestException("orderId " + orderId + " not found! "));
        if(!orderEntity.getUserId().equals(user.getUserId())) throw new BadRequestException("userId "+user.getUserId()+" is not matches with orderEntity userId !");
        if(!orderEntity.getUserEmail().equals(user.getEmail())) throw new BadRequestException("email "+user.getEmail()+" is not matches with orderEntity email!");
        if (orderEntity.getStatus() != Status.PAYMENT_PENDING && orderEntity.getStatus() != Status.PAYMENT_FAILED) {
            throw new BadRequestException("status is not matches with orderEntity status!");
        }
        List<OrderPlace> orderPlaces = convertInDto(List.of(orderEntity));
        System.out.println("Payment want this order: "+orderPlaces);
        return orderPlaces.get(0);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userOrder", allEntries = true),
            @CacheEvict(value = "allOrders", allEntries = true)
    })
    public void paymentSucceeded(Long orderId) {
        OrderEntity order = orderRepo.findById(orderId).orElseThrow(() -> new BadRequestException("orderId " + orderId + " not found!"));
        if (order.getStatus() == Status.PAID) return;
        if (order.getStatus() != Status.PAYMENT_PENDING && order.getStatus() != Status.PAYMENT_FAILED) {
            throw new BadRequestException("Order is not awaiting payment!");
        }
        inventoryClient.confirm(orderId);
        orderRepo.updateStatus(orderId, Status.PAID);
        order.setStatus(Status.PAID);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userOrder", allEntries = true),
            @CacheEvict(value = "allOrders", allEntries = true)
    })
    public void paymentFailed(Long orderId) {
        OrderEntity order = orderRepo.findById(orderId).orElseThrow(() -> new BadRequestException("orderId " + orderId + " not found!"));
        if (order.getStatus() == Status.PAYMENT_FAILED) return;
        if (order.getStatus() != Status.PAYMENT_PENDING && order.getStatus() != Status.CANCELLED) {
            throw new BadRequestException("Order is not awaiting payment!");
        }
        inventoryClient.clearPaymentStarted(orderId);
        orderRepo.updateStatus(orderId, Status.PAYMENT_FAILED);
        order.setStatus(Status.PAYMENT_FAILED);
    }
}
