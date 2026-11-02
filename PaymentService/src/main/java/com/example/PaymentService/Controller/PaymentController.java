package com.example.PaymentService.Controller;


import com.example.PaymentService.Dto.PaymentResponseDto;
import com.example.PaymentService.Service.PaymentService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping("/create/{orderId}")
    public ResponseEntity<PaymentResponseDto> create(@PathVariable Long orderId, @RequestHeader("X-Authenticated-Email") String email,
                                                     @RequestHeader("X-Authenticated-User-Id") Long userId) throws RazorpayException {
        return ResponseEntity.ok().body(paymentService.create(orderId,userId,email));
    }
}
