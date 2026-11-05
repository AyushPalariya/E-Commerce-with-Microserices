package com.example.PaymentService.Repository;

import com.example.PaymentService.Dto.PaymentStatusOrder;
import com.example.PaymentService.Entities.PaymentEntity;
import com.example.PaymentService.Entities.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentRepo extends JpaRepository<PaymentEntity,Long> {
    Optional<PaymentEntity> findByOrderId(Long orderId);

    @Modifying
    @Query("""
        Update PaymentEntity p Set p.status= :status,
        p.raz_payment_id= :razPaymentId,p.updatedAt= :updatedAt where p.raz_order_id= :razOrderId
""")
    int setStatusAndPaymentId(@Param("status") Status status,@Param("updatedAt") LocalDateTime updatedAt, @Param("razPaymentId") String paymentId,
                              @Param("razOrderId") String orderId);

    @Query("""
        SELECT new com.example.PaymentService.Dto.PaymentStatusOrder(
        p.orderId, p.status
        ) from PaymentEntity p where p.raz_order_id= :razOrderId
""")
    Optional<PaymentStatusOrder> findOrderIdAndStatus(@Param("razOrderId") String razOrderId);
}
