package com.example.AuthService.Controller;

import com.example.AuthService.DTO.*;
import com.example.AuthService.Service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    @DeleteMapping("/delete/{email}")
    public ResponseEntity<String> delete(@PathVariable("email") String email, HttpServletResponse res){
        return ResponseEntity.status(HttpStatus.OK).body(authService.delete(email,res));
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<Map<String,String>> forgotPassword(@RequestBody Map<String,String> mp){
        return ResponseEntity.status(HttpStatus.OK).body(authService.forgot(mp));
    }
    @PostMapping("/valid-otp")
    public ResponseEntity<Map<String,String>> passwordReset(@RequestBody OtpToken otpToken){
        return ResponseEntity.status(HttpStatus.OK).body(authService.validateOtp(otpToken));
    }
    @PatchMapping("/password-reset")
    public ResponseEntity<Map<String,String>> passwordReset(@RequestBody PasswordResetRequest resetRequest){
        return ResponseEntity.status(HttpStatus.OK).body(authService.resetPassword(resetRequest));
    }

}
