package com.example.OrderService.Entities;

public enum Status {
    PLACED,
    INVENTORY_PENDING,
    INVENTORY_FAILED,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    PAID,
    PACKED,
    SHIPPED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    RETURNED
}
