package com.example.PaymentService.Service;


import com.example.PaymentService.Dto.OrderPlace;
import com.example.PaymentService.Dto.UserDetailsDto;
import com.example.PaymentService.Repository.OrderClientInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    @Autowired
    private OrderClientInterface orderClientInterface;
    @CachePut(value = "orderPlace",key = "#orderId")
    public OrderPlace putOrderPlace(Long orderId, UserDetailsDto userDetailsDto){
        return orderClientInterface.getOrder(orderId,userDetailsDto).getBody();
    }
    @Cacheable(value = "orderPlace",key="#orderId")
    public OrderPlace getOrderPlace(Long orderId){
        return null;
    }
    @CacheEvict(value = "orderPlace",key = "#orderId")
    public void remove(Long orderId) {
        System.out.println("Order present in cache,it is removed..");
    }
}
