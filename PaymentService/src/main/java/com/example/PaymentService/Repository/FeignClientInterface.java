package com.example.PaymentService.Repository;

import com.example.PaymentService.Dto.PaymentOrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name="ORDERSERVICE")
public interface FeignClientInterface {

    @GetMapping("/order/internal/{orderId}")
    ResponseEntity<PaymentOrderDto> getOrderStatusAndAmount(Long orderId, Long userId, String email);
}
