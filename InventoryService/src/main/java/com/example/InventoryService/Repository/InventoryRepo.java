package com.example.InventoryService.Repository;

import com.example.InventoryService.Entities.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepo extends JpaRepository<InventoryEntity,Long> {
    Optional<InventoryEntity> findByProductId(Long productId);



    List<InventoryEntity> findAllByProductIdIn(List<Long> productIds);
}
