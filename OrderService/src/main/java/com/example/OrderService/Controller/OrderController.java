package com.example.OrderService.Controller;

import com.example.OrderService.Dto.OrderPlace;
import com.example.OrderService.Dto.UserDetailsDto;
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

    @DeleteMapping("/delete/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId,@RequestHeader("X-Authenticated-User-Id") Long userId){
        orderService.deleteOrder(orderId,userId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/internal/{orderId}")
    public ResponseEntity<OrderPlace> getOrder(@PathVariable Long orderId,@RequestBody UserDetailsDto userDetails){
        return new ResponseEntity<>(orderService.getOrder(orderId,userDetails), HttpStatus.OK);
    }
}
