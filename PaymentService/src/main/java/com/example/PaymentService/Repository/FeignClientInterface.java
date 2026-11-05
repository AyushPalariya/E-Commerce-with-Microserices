package com.example.PaymentService.Repository;

import com.example.PaymentService.Dto.OrderPlace;
import com.example.PaymentService.Dto.UserDetailsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="ORDERSERVICE")
public interface FeignClientInterface {

    @PostMapping("/order/internal/{orderId}")
    ResponseEntity<OrderPlace> getOrder(@PathVariable Long orderId, @RequestBody UserDetailsDto userDetails);
}
