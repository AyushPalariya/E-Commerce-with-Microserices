package com.example.PaymentService.Repository;

import com.example.PaymentService.Dto.OrderPlace;
import com.example.PaymentService.Dto.UserDetailsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="ORDERSERVICE")
public interface OrderClientInterface {

    @PostMapping("/order/internal/{orderId}")
    ResponseEntity<OrderPlace> getOrder(@PathVariable Long orderId, @RequestBody UserDetailsDto userDetails);

    @PostMapping("/order/internal/{orderId}/payment-success")
    void paymentSucceeded(@PathVariable("orderId") Long orderId);

    @PostMapping("/order/internal/{orderId}/payment-failed")
    void paymentFailed(@PathVariable("orderId") Long orderId);
}
