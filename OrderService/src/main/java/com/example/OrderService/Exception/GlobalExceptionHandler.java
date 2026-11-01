package com.example.OrderService.Exception;

import com.example.OrderService.Dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        ApiError error = new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST,
                "Bad request", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }
}
