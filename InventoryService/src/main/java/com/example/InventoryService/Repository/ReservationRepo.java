package com.example.InventoryService.Repository;

import com.example.InventoryService.Entities.ReservationEntity;
import com.example.InventoryService.Entities.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepo extends JpaRepository<ReservationEntity,Long> {
    List<ReservationEntity> findByStatus(Status status);
    List<ReservationEntity> findByOrderIdAndStatus(Long orderId, Status status);
}
