package com.example.AuthService.Controller;

import com.example.AuthService.DTO.*;
import com.example.AuthService.Service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth APIs",description = "RestApis for managing Auth")
public class AuthController {
    private final AuthService authService;
    //for verification of email
    @Operation(summary = "Send otp to verify emailAddress",description = "Give email in body as a json"
    ,requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Email Example",
                            value = """
                    {
                        "email": "harsh.r@zecdata.com"
                    }
                    """
                    )
            )
    ))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                {
                    "email": "harsh.r@zecdata.com",
                    "datetime": "2026-11-20T10:30:00"
                }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid email"
            )
    })
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String,String>> sendOtp(@RequestBody Map<String,String> mp){
        return ResponseEntity.status(HttpStatus.OK).body(authService.sendOtp(mp));
    }


    @Operation(summary = "Registration request",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Registration Example",
                                    value = """
                    {
                        "email": "harsh.r@zecdata.com",
                        "name":"harsh",
                        "phone_no":"4567323432",
                        "password":"***"
                    }
                    """
                            )
                    )
            ))
    @PostMapping("/register")
    public ResponseEntity<UserRegistration> registration(@RequestBody UserRegistration userRegistration){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(userRegistration));
    }

    @Operation(summary = "Login Request")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody UserLoginRequest userLogin,HttpServletResponse response){
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(userLogin,response));
    }

    @Operation(summary = "Refresh token api",description = "Read RefreshToken from Cookies")
    @SecurityRequirement(name = "refreshCookie")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody(required = false) String refreshToken, HttpServletRequest request,HttpServletResponse response){
        return ResponseEntity.status(HttpStatus.OK).body(authService.refreshToken(refreshToken,request,response));
    }

    @Operation(summary = "Logout Request",description = "Read RefreshToken from Cookies")
    @SecurityRequirement(name = "refreshCookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req,HttpServletResponse res){
        authService.logout(req,res);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    @Operation(summary = "User Service call this api internally")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/delete/{email}")
    public ResponseEntity<String> delete(@PathVariable("email") String email, HttpServletResponse res){
        return ResponseEntity.status(HttpStatus.OK).body(authService.delete(email,res));
    }
    @Operation(summary = "ForgotPassword ",description = "Give email as a json",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forgot Example",
                                    value = """
                    {
                        "email": "harsh.r@zecdata.com"
                    }
                    """
                            )
                    )
            ))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                {
                    "email": "harsh.r@zecdata.com",
                    "datetime": "2026-11-20T10:30:00"
                }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid email"
            )
    })
    @PostMapping("/forgotPassword")
    public ResponseEntity<Map<String,String>> forgotPassword(@RequestBody Map<String,String> mp){
        return ResponseEntity.status(HttpStatus.OK).body(authService.forgot(mp));
    }

    @Operation(summary = "ValidateOtp",description = "Give datetime that comes from forgotPassword api")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP Correct",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                {
                    "SUCCESS":"OTP Correct"
                }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid otp"
            )
    })
    @PostMapping("/valid-otp")
    public ResponseEntity<Map<String,String>> passwordResetOrVerifyOtp(@RequestBody OtpToken otpToken){
        return ResponseEntity.status(HttpStatus.OK).body(authService.validateOtp(otpToken));
    }

    @Operation(summary = "Password Reset after validateOtp")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password has been changed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                {
                    "SUCCESS":"Password changed"
                }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Password won't changed"
            )
    })
    @PatchMapping("/password-reset")
    public ResponseEntity<Map<String,String>> passwordReset(@RequestBody PasswordResetRequest resetRequest){
        return ResponseEntity.status(HttpStatus.OK).body(authService.resetPassword(resetRequest));
    }

}
