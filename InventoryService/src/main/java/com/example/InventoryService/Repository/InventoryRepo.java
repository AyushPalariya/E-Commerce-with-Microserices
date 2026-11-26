package com.example.InventoryService.Repository;

import com.example.InventoryService.Entities.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepo extends JpaRepository<InventoryEntity,Long> {
    java.util.Optional<InventoryEntity> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<InventoryEntity> findAllByProductIdIn(List<Long> productIds);
}
