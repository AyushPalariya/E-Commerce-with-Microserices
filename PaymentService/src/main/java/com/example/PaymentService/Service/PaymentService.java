package com.example.PaymentService.Service;

import com.example.PaymentService.Dto.PaymentOrderDto;
import com.example.PaymentService.Dto.PaymentResponseDto;
import com.example.PaymentService.Entities.PaymentEntity;
import com.example.PaymentService.Entities.Status;
import com.example.PaymentService.GlobalException.BadRequestExceptionClass;
import com.example.PaymentService.Repository.FeignClientInterface;
import com.example.PaymentService.Repository.PaymentRepo;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {
    @Autowired
    private FeignClientInterface feignClientInterface;
    @Autowired
    private RazorpayClient razorpayClient;
    @Autowired
    private PaymentRepo paymentRepo;
    @Value("${razorpay.key.id}")
    private String keyId;
    public PaymentResponseDto create(Long orderId, Long userId, String email) throws RazorpayException {
        validate(orderId,userId,email);
        PaymentOrderDto paymentOrder = feignClientInterface.getOrderStatusAndAmount(orderId, userId, email).getBody();

            JSONObject order=new JSONObject();
            order.put("amount",paymentOrder.getTotalAmount()*100);
            order.put("currency","INR");
            order.put("receipt","txn_"+ UUID.randomUUID());
            Order orderDetails = razorpayClient.orders.create(order);
            String raz_order_id=orderDetails.get("id");
            PaymentEntity p=new PaymentEntity(orderId,raz_order_id,paymentOrder.getTotalAmount(), Status.PENDING,email,userId);
            PaymentEntity save = paymentRepo.save(p);
            return new PaymentResponseDto(raz_order_id, save.getAmount(), keyId);
    }

    private void validate(Long orderId, Long userId, String email) {
        if(orderId==null||orderId<=0) throw new BadRequestExceptionClass("orderId must be greater than zero!");
        if(userId==null||userId<=0) throw new BadRequestExceptionClass("userId must be greater than zero!");
        if(email==null||email.isBlank()) throw new BadRequestExceptionClass("email is required!");
    }

}
