package com.example.OrderService.Controller;

import com.example.OrderService.Dto.OrderPlace;
import com.example.OrderService.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @PostMapping("/place")
    public ResponseEntity<OrderPlace> orderPlace(@RequestBody OrderPlace orderPlace){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.orderPlace(orderPlace));
    }
    @GetMapping("/getAllOrders")
    public ResponseEntity<List<OrderPlace>> getAllOrder(){
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getAllOrder());
    }
    @GetMapping("/user-order/{userid}")
    public ResponseEntity<List<OrderPlace>> getUserOrder(@PathVariable Long userid){
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getUserOrder(userid));
    }
}
