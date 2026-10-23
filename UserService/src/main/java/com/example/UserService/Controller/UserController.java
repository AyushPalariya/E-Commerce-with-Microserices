package com.example.UserService.Controller;

import com.example.UserService.DTO.ProfileRequest;
import com.example.UserService.Entities.UserProfile;
import com.example.UserService.Service.UserService;
import com.netflix.discovery.converters.Auto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @PatchMapping("/update/{id}")
    public ResponseEntity<UserProfile> updateProfile(@PathVariable Long id,@RequestBody Map<String,Object> fieldProfile){
        return new ResponseEntity<>(userService.updateProfile(id,fieldProfile), HttpStatus.OK);
    }
    @GetMapping("/getAllProfile")
    public ResponseEntity<List<UserProfile>> getAll(){
        return new ResponseEntity<>(userService.getAllUserProfile(), HttpStatus.OK);
    }
    @GetMapping("/profile/{id}")
    public ResponseEntity<UserProfile> getProfile(@PathVariable Long id){
        return new ResponseEntity<>(userService.getProfile(id), HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/get")
    public ResponseEntity<UserProfile> get(HttpServletRequest req){
        return new ResponseEntity<>(userService.get(req),HttpStatus.OK);
    }
}
