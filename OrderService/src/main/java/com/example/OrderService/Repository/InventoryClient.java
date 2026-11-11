package com.example.OrderService.Repository;

import com.example.OrderService.Dto.InventoryItem;
import com.example.OrderService.Dto.ItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "INVENTORYSERVICE")
public interface InventoryClient {

    @PostMapping("/inventory/reserved/{orderId}")
    void reserved(@PathVariable("orderId") Long orderId,@RequestBody List<InventoryItem> itemDto);
}
