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
    @ExceptionHandler(BadCredentialClass.class)
    public ResponseEntity<ApiError> handleBadCredentials(HttpServletRequest req,BadCredentialClass ex){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST,ex.getMessage(),"Bad Credentials",req.getRequestURI());
        return new ResponseEntity<>(error,HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(HttpServletRequest req,ConflictException ex){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST,ex.getMessage(),"Conflict",req.getRequestURI());
        return new ResponseEntity<>(error,HttpStatus.CONFLICT);
    }
}
