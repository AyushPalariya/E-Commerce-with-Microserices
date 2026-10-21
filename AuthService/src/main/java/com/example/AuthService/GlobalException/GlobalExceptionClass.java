package com.example.AuthService.GlobalException;

import com.example.AuthService.DTO.ApiError;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionClass {
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredential(BadCredentialsException ex, HttpServletRequest request){
        ApiError error=new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST,"Bad Credentials", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(ConflictException ex,HttpServletRequest req){
        ApiError error=new ApiError(LocalDateTime.now(),HttpStatus.CONFLICT,"Email Conflict",ex.getMessage(),req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(UsernameNotFoundException ex,HttpServletRequest req){
        ApiError error=new ApiError(LocalDateTime.now(),HttpStatus.NOT_FOUND,"Resource not found",ex.getMessage(),req.getRequestURI());
        return new ResponseEntity<>(error,HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(AuthenticationExceptionClass.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationExceptionClass ex,HttpServletRequest req){
        ApiError error=new ApiError(LocalDateTime.now(),HttpStatus.UNAUTHORIZED,"Refresh token error",ex.getMessage(),req.getRequestURI());
        return new ResponseEntity<>(error,HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwt(JwtException ex,HttpServletRequest req){
        ApiError e=new ApiError(LocalDateTime.now(),HttpStatus.UNAUTHORIZED,"Invalid Jwt Token",ex.getMessage(),req.getRequestURI());
        return new ResponseEntity<>(e,HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiError> handleJwt(ExpiredJwtException ex, HttpServletRequest req){
        ApiError e=new ApiError(LocalDateTime.now(),HttpStatus.UNAUTHORIZED,"Token expired",ex.getMessage(),req.getRequestURI());
        return new ResponseEntity<>(e,HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(
            Exception ex,
            HttpServletRequest req
    ) {
        ApiError e=new ApiError(LocalDateTime.now(),HttpStatus.INTERNAL_SERVER_ERROR,"Internal Error",ex.getMessage(),req.getRequestURI());
        return ResponseEntity.internalServerError().body(e);
    }




}
