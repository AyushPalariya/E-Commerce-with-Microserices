package com.example.InventoryService.Controller;

import com.example.InventoryService.Dto.Inventory;
import com.example.InventoryService.Dto.InventoryItem;
import com.example.InventoryService.Service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    @Autowired
    private InventoryService inventoryService;
    @PostMapping("/add")
    public ResponseEntity<List<Inventory>> add(@RequestBody List<Inventory> inventory){
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.add(inventory));
    }
    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody List<Long> ids){
        inventoryService.delete(ids);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    @PostMapping("/reserved/{orderId}")
    public ResponseEntity<Void> reserved(@PathVariable Long orderId, @RequestBody List<InventoryItem> items){
        inventoryService.reserved(orderId,items);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    @GetMapping("/stocks")
    public ResponseEntity<List<Inventory>> getAllStock(){
        return ResponseEntity.ok(inventoryService.getAllStock());
    }
    @PutMapping("/update")
    public ResponseEntity<List<Inventory>> update(List<InventoryItem> inventoryItems){
        return new ResponseEntity<>(inventoryService.updateInventory(inventoryItems),HttpStatus.OK);
    }

}
