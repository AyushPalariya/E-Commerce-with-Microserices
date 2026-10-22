package com.example.UserService.Controller;

import com.example.UserService.DTO.ProfileRequest;
import com.example.UserService.Service.UserService;
import com.netflix.discovery.converters.Auto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @PostMapping("/profile")
    public ResponseEntity<String> createProfile(@RequestBody ProfileRequest profile){
        System.out.println(profile);
        return ResponseEntity.ok(userService.createProfile(profile));
    }
}
