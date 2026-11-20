package com.example.UserService.Controller;

import com.example.UserService.DTO.ProfileRequest;
import com.example.UserService.Entities.UserProfile;
import com.example.UserService.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Tag(name = "User APIs",description = "APIs for managing user")
public class UserController {
    @Autowired
    private UserService userService;
    @Operation(summary = "Auth service internally call this api")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/profile")
    public ResponseEntity<String> createProfile(@RequestBody ProfileRequest profile){
        System.out.println(profile);
        return ResponseEntity.ok(userService.createProfile(profile));
    }
    @Operation(summary = "Update user profile",description = "Required Access token of User or Admin",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Provide the fields you want to update",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Update Profile",
                                    value = """
                {
                    "address": "123 MG Road",
                    "img": "https://example.com/profile.jpg",
                    "state": "Madhya Pradesh",
                    "city": "Bhopal",
                    "country": "India"
                }
                """
                            )
                    )
            ))
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/update")
    public ResponseEntity<UserProfile> updateProfile(@RequestHeader("X-Authenticated-Email") String email,@RequestBody Map<String,Object> fieldProfile){
        return new ResponseEntity<>(userService.updateProfile(email,fieldProfile), HttpStatus.OK);
    }
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all Profile of users",description = "Required Access token of Admin")
    @GetMapping("/getAllProfile")
    public ResponseEntity<List<UserProfile>> getAll(){
        return new ResponseEntity<>(userService.getAllUserProfile(), HttpStatus.OK);
    }
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get user profile",description = "Required Access token of User or Admin")
    @GetMapping("/me")
    public ResponseEntity<UserProfile> getMyProfile(
            @RequestHeader("X-Authenticated-Email") String email) {
        return ResponseEntity.ok(userService.getProfileByEmail(email));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete user Profile",description = "Required Access token of User or Admin")
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteProfile(@RequestHeader("X-Authenticated-Email") String email){
        ResponseEntity<String> auth = userService.deleteUser(email);
        return ResponseEntity.noContent().headers(headers -> headers.addAll(auth.getHeaders())).build();
        //or
//        HttpHeaders headers = new HttpHeaders();
//        headers.add(HttpHeaders.SET_COOKIE,
//                auth.getHeaders().get(HttpHeaders.SET_COOKIE));
//        return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
    }
}
