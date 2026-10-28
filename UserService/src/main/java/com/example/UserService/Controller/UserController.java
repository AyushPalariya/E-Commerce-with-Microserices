package com.example.UserService.Controller;

import com.example.UserService.DTO.ProfileRequest;
import com.example.UserService.Entities.UserProfile;
import com.example.UserService.Service.UserService;
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
    @PatchMapping("/update")
    public ResponseEntity<UserProfile> updateProfile(@RequestHeader("X-Authenticated-Email") String email,@RequestBody Map<String,Object> fieldProfile){
        return new ResponseEntity<>(userService.updateProfile(email,fieldProfile), HttpStatus.OK);
    }
    @GetMapping("/getAllProfile")
    public ResponseEntity<List<UserProfile>> getAll(){
        return new ResponseEntity<>(userService.getAllUserProfile(), HttpStatus.OK);
    }
    @GetMapping("/me")
    public ResponseEntity<UserProfile> getMyProfile(
            @RequestHeader("X-Authenticated-Email") String email) {
        return ResponseEntity.ok(userService.getProfileByEmail(email));
    }
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteProfile(@RequestHeader("X-Authenticated-Email") String email){
        userService.deleteUser(email);
        return ResponseEntity.noContent().build();
    }
}
