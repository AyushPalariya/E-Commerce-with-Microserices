package com.example.OrderService.Service;

import com.example.OrderService.Dto.OrderPlace;
import com.example.OrderService.Dto.ItemDto;
import com.example.OrderService.Entities.OrderEntity;
import com.example.OrderService.Entities.OrderItem;
import com.example.OrderService.Entities.Status;
import com.example.OrderService.Exception.BadRequestException;
import com.example.OrderService.Repository.OrderRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class OrderService {
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private KafkaTemplate<String,OrderPlace> template;
    @Value("${kafka.topic.order}")
    private String topic;
    @CacheEvict(value = "userOrder",allEntries = true)
    @Transactional
    public OrderPlace orderPlace(OrderPlace orderPlace,String email,Long id){
        validateOrder(orderPlace, email, id);
        OrderEntity order = new OrderEntity();
        order.setUserId(id);
        order.setUserEmail(email);
        order.setUserName(orderPlace.getName());
        order.setDatetime(LocalDateTime.now());
        order.setStatus(Status.PLACED);
        order.setTotalAmount(orderPlace.getTotalAmount());

        List<OrderItem> items = new ArrayList<>();
        for (ItemDto itemDto : orderPlace.getItems()) {

            OrderItem item = new OrderItem();
            item.setProduct_id(itemDto.getProduct_id());
            item.setProduct_name(itemDto.getProduct_name());
            item.setQuantities(itemDto.getQuantities());
            item.setSubAmount(itemDto.getSubAmount());
            item.setOrder(order);
            log.info(item.getOrder().toString());
            items.add(item);
        }
        order.setItems(items);

        /*
         payment service,when complete then
         */

        OrderEntity save = orderRepo.save(order);
        List<ItemDto> itemDto=save.getItems().stream().map(item -> {
            ItemDto savedItem = new ItemDto();
            savedItem.setId(item.getId());
            savedItem.setProduct_id(item.getProduct_id());
            savedItem.setProduct_name(item.getProduct_name());
            savedItem.setQuantities(item.getQuantities());
            savedItem.setSubAmount(item.getSubAmount());
            return savedItem;
        }).toList();
        OrderPlace orderPlace1 = new OrderPlace(save.getId(), save.getUserId(), save.getUserEmail(), orderPlace.getName(),
                save.getTotalAmount(), String.valueOf(save.getStatus()), save.getDatetime(), itemDto);
        try {
            template.send(topic, email, orderPlace1).get();
            log.info("Order notification published for orderId={}", save.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing order notification", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to publish order notification", e.getCause());
        }

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
        if (orderPlace.getTotalAmount() == null || !Double.isFinite(orderPlace.getTotalAmount())
                || orderPlace.getTotalAmount() < 0)
            throw new BadRequestException("Order total must be a non-negative number!");
        if (orderPlace.getItems() == null || orderPlace.getItems().isEmpty())
            throw new BadRequestException("An order must contain at least one item!");
        for (ItemDto item : orderPlace.getItems()) {
            if (item == null) throw new BadRequestException("Order item details are required!");
            if (item.getProduct_id() == null || item.getProduct_id() <= 0)
                throw new BadRequestException("Each order item must have a positive product id!");
            if (item.getProduct_name() == null || item.getProduct_name().isBlank())
                throw new BadRequestException("Each order item must have a product name!");
            if (item.getQuantities() <= 0)
                throw new BadRequestException("Each order item quantity must be positive!");
            if (!Double.isFinite(item.getSubAmount()) || item.getSubAmount() < 0)
                throw new BadRequestException("Each order item amount must be a non-negative number!");
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

}
