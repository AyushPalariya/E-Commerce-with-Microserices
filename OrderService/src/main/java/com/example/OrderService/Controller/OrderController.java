package com.example.OrderService.Controller;

import com.example.OrderService.Dto.OrderPlace;
import com.example.OrderService.Dto.UserDetailsDto;
import com.example.OrderService.Service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@Tag(name = "Order APIs",description = "RestApis for managing orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Value("${server.port}")
    private String port;
    @Operation(summary = "Order Place Api",description = "Required accessToken of Admin or User in Authorization",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Order Example",
                                    value = """
                                            {
                                              "name": "Ayush",
                                              "items": [
                                                {
                                                  "product_id": 153,
                                                  "quantities": 2,
                                                  "color": "Light Blue",
                                                  "size": "L"
                                                },
                                                {
                                                  "product_id": 157,
                                                  "quantities": 1,
                                                  "color": "Navy Blue",
                                                  "size": "M"
                                                }
                                              ]
                                            }
                    """
                            )
                    )
            ))
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/place")
    public ResponseEntity<OrderPlace> orderPlace(
            @RequestHeader("X-Authenticated-Email") String email,
            @RequestHeader("X-Authenticated-User-Id") Long userId,
            @RequestBody OrderPlace orderPlace){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.orderPlace(orderPlace,email,userId));
    }
    //admin
    @Operation(summary = "Get All Orders",description = "Give accessToken of Admin in Authorization.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/getAllOrders")
    public ResponseEntity<List<OrderPlace>> getAllOrder(){
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getAllOrder());
    }
    @Operation(summary = "Get my Orders",description = "Give accessToken of User or Admin in Authorization ")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderPlace>> getOrderByUserId(
            @RequestHeader("X-Authenticated-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.getOrderByUserId(userId));
    }
    @GetMapping("/test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok().body(port);
    }
    @Operation(summary = "Delete order with orderId",description = "Required OrderId as a PathVariable and accessToken of User or Admin in Authorization")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/delete/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId,@RequestHeader("X-Authenticated-User-Id") Long userId){
        orderService.deleteOrder(orderId,userId);
        return ResponseEntity.noContent().build();
    }
    @Operation(summary = "Payment Service call this api internally")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/internal/{orderId}")
    public ResponseEntity<OrderPlace> getOrder(@PathVariable Long orderId,@RequestBody UserDetailsDto userDetails){
        return new ResponseEntity<>(orderService.getOrder(orderId,userDetails), HttpStatus.OK);
    }
    @Operation(summary = "Payment Service call this api internally")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/internal/{orderId}/payment-success")
    public ResponseEntity<Void> paymentSucceeded(@PathVariable Long orderId){
        orderService.paymentSucceeded(orderId);
        return ResponseEntity.noContent().build();
    }
    @Operation(summary = "Payment Service call this api internally")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/internal/{orderId}/payment-failed")
    public ResponseEntity<Void> paymentFailed(@PathVariable Long orderId){
        orderService.paymentFailed(orderId);
        return ResponseEntity.noContent().build();
    }
}
