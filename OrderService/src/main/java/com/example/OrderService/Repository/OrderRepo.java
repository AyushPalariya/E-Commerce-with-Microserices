package com.example.OrderService.Repository;

import com.example.OrderService.Entities.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepo extends JpaRepository<OrderEntity,Long > {
    @Query("Select Distinct o From OrderEntity o Left Join Fetch o.items")
    List<OrderEntity> findAllOrderWithItems();

    @Query("Select Distinct o From OrderEntity o Left Join Fetch o.items Where o.userId = :userId")
    List<OrderEntity> findOrdersByUserIdWithItems(@Param("userId") Long userId);
}
