package com.example.ProductService.Dto;



import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ApiError(
        LocalDateTime timestamp,
        HttpStatus status,
        String error,
        String message,
        String path
) {
}
