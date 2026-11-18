package com.example.InventoryService.Controller;

import com.example.InventoryService.Dto.Inventory;
import com.example.InventoryService.Dto.InventoryItem;
import com.example.InventoryService.Entities.ReservationEntity;
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
    @GetMapping("/maxSold")
    public ResponseEntity<List<Long>> maxSold(){
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.maxSold());
    }
    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody List<Long> ids){
        inventoryService.delete(ids);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    @PostMapping("/reserved/{orderId}")
    public ResponseEntity<Void> reservedFromOrder(@PathVariable Long orderId, @RequestBody List<InventoryItem> items){
        inventoryService.reserved(orderId,items);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    @PostMapping("/released/{orderId}")
    public ResponseEntity<Void> releaseReservation(@PathVariable Long orderId){
        inventoryService.release(orderId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<Void> confirmReservation(@PathVariable Long orderId){
        inventoryService.confirm(orderId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/payment-started/{orderId}")
    public ResponseEntity<Void> paymentStarted(@PathVariable Long orderId){
        inventoryService.markPaymentStarted(orderId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/payment-aborted/{orderId}")
    public ResponseEntity<Void> paymentAborted(@PathVariable Long orderId){
        inventoryService.clearPaymentStarted(orderId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/stocks")
    public ResponseEntity<List<Inventory>> getAllStock(){
        return ResponseEntity.ok(inventoryService.getAllStock());
    }
    @PatchMapping("/update")
    public ResponseEntity<List<Inventory>> update(@RequestBody List<InventoryItem> inventoryItems){
        return new ResponseEntity<>(inventoryService.updateInventory(inventoryItems),HttpStatus.OK);
    }
    @PostMapping("/stock")                              //productId only from request
    public ResponseEntity<Inventory> getStock(@RequestBody Inventory inventory){
        return new ResponseEntity<>(inventoryService.getStock(inventory),HttpStatus.OK);
    }
    @GetMapping("/reserved")
    public ResponseEntity<List<ReservationEntity>> getReservedRecords(){
        return new ResponseEntity<>(inventoryService.getReservedRecords(),HttpStatus.OK);
    }
    @GetMapping("/released")
    public ResponseEntity<List<ReservationEntity>> getReleasedRecords(){
        return new ResponseEntity<>(inventoryService.getReleasedRecords(),HttpStatus.OK);
    }
    @GetMapping("/confirmed")
    public ResponseEntity<List<ReservationEntity>> getConfirmedRecords(){
        return new ResponseEntity<>(inventoryService.getConfirmedRecords(),HttpStatus.OK);
    }


}
