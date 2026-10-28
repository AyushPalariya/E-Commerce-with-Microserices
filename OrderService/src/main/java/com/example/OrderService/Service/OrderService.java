package com.example.OrderService.Service;

import com.example.OrderService.Dto.OrderPlace;
import com.example.OrderService.Dto.ItemDto;
import com.example.OrderService.Entities.OrderEntity;
import com.example.OrderService.Entities.OrderItem;
import com.example.OrderService.Entities.Status;
import com.example.OrderService.Repository.OrderRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepo orderRepo;

    @Transactional
    public OrderPlace orderPlace(OrderPlace orderPlace){
        OrderEntity order = new OrderEntity();
        order.setUserId(orderPlace.getUserId());
        order.setUserEmail(orderPlace.getUserEmail());
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
            items.add(item);
        }
        order.setItems(items);

        /**
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
        //email here
        return new OrderPlace(save.getId(), save.getUserId(), orderPlace.getUserEmail(), orderPlace.getName(),
                save.getTotalAmount(), String.valueOf(save.getStatus()), save.getDatetime(),itemDto);

    }

    @Transactional(readOnly = true)
    public List<OrderPlace> getAllOrder() {

    }

    public List<OrderPlace> getUserOrder(Long userid) {

    }
}
