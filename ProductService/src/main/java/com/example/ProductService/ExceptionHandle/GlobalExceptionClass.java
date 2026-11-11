package com.example.ProductService.ExceptionHandle;

import com.example.ProductService.Dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionClass {
    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<ApiError> handleResource(ResourceNotFound ex, HttpServletRequest req){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.NOT_FOUND,ex.getMessage(),"Resource not found!",req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest req){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST,"Bad request",ex.getMessage(),req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex,HttpServletRequest req){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.CONFLICT,"Conflict",ex.getMessage(),req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    @ExceptionHandler(ProductIdsNotFound.class)
    public ResponseEntity<ApiError> handleProduct(ProductIdsNotFound ex,HttpServletRequest req){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.NOT_FOUND,"ids not found",ex.getMessage(),req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
