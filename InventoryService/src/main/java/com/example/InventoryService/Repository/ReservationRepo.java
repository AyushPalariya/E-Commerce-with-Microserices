package com.example.InventoryService.Repository;

import com.example.InventoryService.Entities.ReservationEntity;
import com.example.InventoryService.Entities.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepo extends JpaRepository<ReservationEntity,Long> {
    List<ReservationEntity> findByStatus(Status status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ReservationEntity> findByOrderIdAndStatus(Long orderId, Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationEntity r where r.status = :status and r.expiredAt <= :now and (r.paymentStarted = false or r.paymentStarted is null)")
    List<ReservationEntity> findExpiredUnpaid(@Param("status") Status status, @Param("now") LocalDateTime now);
}
