package com.example.PaymentService.Repository;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "INVENTORYSERVICE", contextId = "paymentInventoryClient")
public interface InventoryClient {
    @PostMapping("/inventory/payment-started/{orderId}")
    void markPaymentStarted(@PathVariable("orderId") Long orderId);

    @PostMapping("/inventory/payment-aborted/{orderId}")
    void clearPaymentStarted(@PathVariable("orderId") Long orderId);
}
