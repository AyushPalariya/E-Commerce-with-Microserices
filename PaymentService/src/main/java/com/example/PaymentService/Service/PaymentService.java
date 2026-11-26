package com.example.PaymentService.Service;

import com.example.PaymentService.Dto.*;
import com.example.PaymentService.Dto.PaymentPackage.PaymentPayLoad;
import com.example.PaymentService.Entities.PaymentEntity;
import com.example.PaymentService.Entities.Status;
import com.example.PaymentService.GlobalException.BadRequestExceptionClass;
import com.example.PaymentService.GlobalException.RazorpayExceptionClass;
import com.example.PaymentService.Repository.PaymentRepo;
import com.example.PaymentService.Repository.OrderClientInterface;
import com.example.PaymentService.Repository.InventoryClient;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {
    @Autowired
    private KafkaTemplate<String, OrderPaymentEmail> template;
    @Value("${kafka.topic.order}")
    private String topic;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private RazorpayClient razorpayClient;
    @Autowired
    private PaymentRepo paymentRepo;
    @Autowired
    private OrderClientInterface orderClient;
    @Autowired
    private InventoryClient inventoryClient;
    @Value("${razorpay.key.id}")
    private String keyId;

    @Transactional
    public PaymentResponseDto create(Long orderId, Long userId, String email) throws RazorpayException {
        validate(orderId,userId,email);

        Optional<PaymentEntity> byOrderId = paymentRepo.findByOrderId(orderId);
        if(byOrderId.isPresent()){
            PaymentEntity pp=byOrderId.get();
            log.info("payment retry for orderId {}",pp.getOrderId());
            if(pp.getStatus()==Status.SUCCESS) throw new RazorpayExceptionClass("Payment already done for this order!");
            inventoryClient.markPaymentStarted(orderId);
            return new PaymentResponseDto(pp.getRaz_order_id(), pp.getAmount()*100, keyId);
        }

        UserDetailsDto userDetails=new UserDetailsDto(email,userId);
        //cache to get orderPlace
        OrderPlace paymentOrder = cacheService.putOrderPlace(orderId, userDetails);
        if (paymentOrder == null) throw new BadRequestExceptionClass("Order was not found or is not payable!");
        inventoryClient.markPaymentStarted(orderId);
        try {
            JSONObject order=new JSONObject();
            order.put("amount",paymentOrder.getTotalAmount()*100);
            order.put("currency","INR");
            order.put("receipt","txn_"+ UUID.randomUUID());
            Order orderDetails = razorpayClient.orders.create(order);
            String raz_order_id=orderDetails.get("id");
            log.info("razorpayOrderId:{}",raz_order_id);
            log.info("amount:{}",(Integer)orderDetails.get("amount"));
            log.info("currency:{}",(String)orderDetails.get("currency"));
            PaymentEntity p=new PaymentEntity(orderId,raz_order_id,paymentOrder.getTotalAmount(), Status.PENDING,email,userId);
            PaymentEntity save = paymentRepo.save(p);
            log.info("new payment for orderId:{}",save.getOrderId());
            return new PaymentResponseDto(raz_order_id, save.getAmount()*100, keyId);
        } catch (RazorpayException | RuntimeException exception) {
            try {
                inventoryClient.clearPaymentStarted(orderId);
            } catch (Exception clearException) {
                exception.addSuppressed(clearException);
                log.error("Unable to clear payment-started marker for orderId {}", orderId, clearException);
            }
            throw exception;
        }
    }

    private void validate(Long orderId, Long userId, String email) {
        if(orderId==null||orderId<=0) throw new BadRequestExceptionClass("orderId must be greater than zero!");
        if(userId==null||userId<=0) throw new BadRequestExceptionClass("userId must be greater than zero!");
        if(email==null||email.isBlank()) throw new BadRequestExceptionClass("email is required!");
    }
    @Transactional
    public void verify(String signature, PaymentPayLoad payload) {

        log.info("Payload {}",payload);
        String raz_status = payload.getPayload().getPayment().getEntity().getStatus();
        log.info("Status :{}",raz_status);
        String raz_orderId = payload.getPayload().getPayment().getEntity().getOrder_id();
        log.info("OrderId: {}",raz_orderId);
        String raz_paymentId= payload.getPayload().getPayment().getEntity().getId();
        log.info("PaymentId:{}",raz_paymentId);
        log.info("Signature {}",signature);

        emailMethodAndUpdate(raz_status,raz_orderId,raz_paymentId);

    }

    private void emailMethodAndUpdate(String raz_status, String raz_orderId, String raz_paymentId) {
        int update=999;

        if(raz_status.equals("failed")){
            PaymentStatusOrder result = paymentRepo.findOrderIdAndStatus(raz_orderId).orElseThrow();
            if (result.getStatus() == Status.SUCCESS) return;
            update = paymentRepo.setStatusAndPaymentId(Status.FAILED,LocalDateTime.now(), raz_paymentId,raz_orderId);
            orderClient.paymentFailed(result.getOrderId()).getBody();
        }
        else if(raz_status.equals("captured")){
            PaymentStatusOrder result = paymentRepo.findOrderIdAndStatus(raz_orderId).orElseThrow();
            Long orderId =result.getOrderId();
            System.out.println(orderId);
            System.out.println(result.getStatus());
            if(result.getStatus()==Status.SUCCESS) {
                orderClient.paymentSucceeded(orderId).getBody();
                return;
            }
            update=paymentRepo.setStatusAndPaymentId(Status.SUCCESS,LocalDateTime.now(),raz_paymentId,raz_orderId);
            orderClient.paymentSucceeded(orderId).getBody();
            //Email kafka
            //getDataFromCacheOfOrderPlace
            OrderPlace cacheOrder = cacheService.getOrderPlace(orderId);
            List<ItemDto> list = cacheOrder.getItems().stream().toList();

            log.info("order get from cache:{}",cacheOrder);
            log.info("List of items:{}",list);
            OrderPaymentEmail emailSend=new OrderPaymentEmail(raz_orderId,raz_paymentId,cacheOrder);
            try {
                template.send(topic, raz_orderId,emailSend);
                System.out.println("email send of orderPlace to "+emailSend.getOrder().getUserEmail());
            }
            catch (Exception e){
                System.out.println("email not send for order place with email :"+emailSend.getOrder().getUserEmail());
            }
            cacheService.remove(orderId);
        }
        log.info("Payment Status updated with "+update+" and Status is "+raz_status);
    }
}
