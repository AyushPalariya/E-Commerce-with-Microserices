package com.example.PaymentService.Controller;


import com.example.PaymentService.Dto.PaymentPackage.PaymentPayLoad;
import com.example.PaymentService.Dto.PaymentResponseDto;
import com.example.PaymentService.Service.PaymentService;
import com.razorpay.RazorpayException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Signature;

@RestController
@RequestMapping("/payment")
@Slf4j
@Tag(name = "Payment APIs",description = "RestApis for managing Payment")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;
    @Operation(summary = "Initial payment Api after orderPlace",description = "Give OrderId as a PathVariable and accessToken of User or Admin")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/initiate/{orderId}")
    public ResponseEntity<PaymentResponseDto> create(@PathVariable Long orderId, @RequestHeader("X-Authenticated-User-Id") Long userId,@RequestHeader("X-Authenticated-Email") String email) throws RazorpayException {
        return ResponseEntity.ok().body(paymentService.create(orderId,userId,email));
    }

    //webhooks
    @Operation(summary = "Automatically call when an event occurs")
    @PostMapping("/webhooks")
    public ResponseEntity<Void> paymentWebhooks(@RequestBody(required = false) PaymentPayLoad payload,
                                                @RequestHeader("X-Razorpay-Signature") String signature){
        log.info("WebHooks called..");
        paymentService.verify(signature,payload);


        return ResponseEntity.noContent().build();
    }
}
