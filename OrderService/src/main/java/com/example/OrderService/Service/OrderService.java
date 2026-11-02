package com.example.OrderService.Service;

import com.example.OrderService.Dto.OrderPlace;
import com.example.OrderService.Dto.ItemDto;
import com.example.OrderService.Dto.PaymentOrderDto;
import com.example.OrderService.Dto.ProductDto;
import com.example.OrderService.Entities.OrderEntity;
import com.example.OrderService.Entities.OrderItem;
import com.example.OrderService.Entities.Status;
import com.example.OrderService.Exception.BadRequestException;
import com.example.OrderService.Repository.FeignClientInterface;
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
    private KafkaTemplate<String,OrderPlace> template;
    @Value("${kafka.topic.order}")
    private String topic;
    @Autowired
    private FeignClientInterface feignClientInterface;
    @CacheEvict(value = "userOrder",allEntries = true)
    @Transactional
    public OrderPlace orderPlace(OrderPlace orderPlace,String email,Long id){
        validateOrder(orderPlace, email, id);
        List<Long> product_ids=orderPlace.getItems().stream().map(ItemDto::getProduct_id).distinct().toList();
        List<ProductDto> products= feignClientInterface.getProducts(product_ids).getBody();
        if (products == null || products.isEmpty()) {
            throw new BadRequestException("Products not found");
        }
        System.out.println(product_ids);
        Map<Long,ProductDto> productMap=new HashMap<>();
        for(ProductDto p:products) {
            System.out.print(p.getId()+" ");
            productMap.put(p.getId(),p);
        }


        OrderEntity order = new OrderEntity();
        order.setUserId(id);
        order.setUserEmail(email);
        order.setUserName(orderPlace.getName());
        order.setDatetime(LocalDateTime.now());
        order.setStatus(Status.PAYMENT_PENDING);

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
            template.send(topic, email, orderPlace1);
            System.out.println("email send of orderPlace to "+email);
        }
        catch (Exception e){
            System.out.println("email not send for order place with email :"+email);
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

    public PaymentOrderDto getOrderWithStatusAndAmount(Long orderId, Long userId, String email) {
        OrderEntity orderEntity = orderRepo.findById(orderId).orElseThrow(() -> new BadRequestException("orderId " + orderId + " not found! "));
        if(!orderEntity.getUserId().equals(userId)) throw new BadRequestException("userId "+userId+" is not matches with orderEntity userId !");
        if(!orderEntity.getUserEmail().equals(email)) throw new BadRequestException(email+" is not matches with orderEntity email!");
        if(!Status.PAYMENT_PENDING.equals(orderEntity.getStatus())) throw new BadRequestException("status is not matches with orderEntity status!");
        return new PaymentOrderDto(orderEntity.getTotalAmount(),orderEntity.getStatus().name());
    }
}
