package com.example.AuthService.Controller;

import com.example.AuthService.DTO.LoginResponse;
import com.example.AuthService.DTO.UserLoginRequest;
import com.example.AuthService.DTO.UserRegistration;
import com.example.AuthService.Service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserRegistration> registration(@RequestBody UserRegistration userRegistration){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(userRegistration));
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody UserLoginRequest userLogin,HttpServletResponse response){
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(userLogin,response));
    }
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody(required = false) String refreshToken, HttpServletRequest request,HttpServletResponse response){
        return ResponseEntity.status(HttpStatus.OK).body(authService.refreshToken(refreshToken,request,response));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req,HttpServletResponse res){
        authService.logout(req,res);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
