package com.example.UserService.GlobalExcep;

import com.example.UserService.DTO.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalException {
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadCredentials(HttpServletRequest req, BadRequestException ex){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST,"Bad Request",ex.getMessage(),req.getRequestURI());
        return new ResponseEntity<>(error,HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(HttpServletRequest req,ConflictException ex){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.CONFLICT,"Conflict",ex.getMessage(),req.getRequestURI());
        return new ResponseEntity<>(error,HttpStatus.CONFLICT);
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(HttpServletRequest req,NotFoundException ex){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.NOT_FOUND,"Resource not found",ex.getMessage(),req.getRequestURI());
        return new ResponseEntity<>(error,HttpStatus.NOT_FOUND);
    }
}
