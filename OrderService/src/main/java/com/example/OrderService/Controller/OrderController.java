package com.example.OrderService.Controller;

import com.example.OrderService.Dto.OrderPlace;
import com.example.OrderService.Entities.OrderEntity;
import com.example.OrderService.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Value("${server.port}")
    private String port;
    @PostMapping("/place")
    public ResponseEntity<OrderPlace> orderPlace(
            @RequestHeader("X-Authenticated-Email") String email,
            @RequestHeader("X-Authenticated-User-Id") Long userId,
            @RequestBody OrderPlace orderPlace){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.orderPlace(orderPlace,email,userId));
    }
    //admin
    @GetMapping("/getAllOrders")
    public ResponseEntity<List<OrderPlace>> getAllOrder(){
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getAllOrder());
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderPlace>> getOrderByUserId(
            @RequestHeader("X-Authenticated-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.getOrderByUserId(userId));
    }
    @GetMapping("/test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok().body(port);
    }

}
