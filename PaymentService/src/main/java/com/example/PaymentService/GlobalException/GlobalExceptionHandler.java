package com.example.PaymentService.GlobalException;

import com.razorpay.RazorpayException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestExceptionClass.class)
    public ResponseEntity<ApiError> handle(BadRequestExceptionClass ex, HttpServletRequest req){
        ApiError e=new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST, ex.getMessage(), "Invalid Inputs",req.getRequestURI());
        return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(RazorpayException.class)
    public ResponseEntity<ApiError> handleRazorpay(RazorpayException ex, HttpServletRequest req){
        ApiError e=new ApiError(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), "Payment Gateway Error",req.getRequestURI());
        return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
